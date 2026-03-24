/**
 * Created By Lavanyaa Karthik
 * Date: 17/02/26
 * Time: 5:52 pm
 */
package com.dev.order.service;

import com.dev.order.audit.OrderAuditService;
import com.dev.order.domain.Order;
import com.dev.order.domain.OrderAction;
import com.dev.order.domain.OrderState;
import com.dev.order.dto.CreateOrderRequest;
import com.dev.order.dto.OrderResponse;
import com.dev.order.dto.PageOrderResponse;
import com.dev.order.exception.InvalidRequestException;
import com.dev.order.exception.OrderNotFoundException;
import com.dev.order.exception.UnauthorizedException;
import com.dev.order.repository.OrderRepository;
import com.dev.order.security.AuthenticatedUser;
import com.dev.order.security.RequestContext;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private final Tracer tracer;
    private final OrderRepository orderRepository;
    private final OrderAuditService orderAuditService;
    private static final int MAX_PAGE_SIZE = 100;

    //Create order
    @Transactional
    @Observed(name = "order.create")
    public OrderResponse createOrder(CreateOrderRequest orderRequest) {
        Long customerId = getCurrentCustomerId();
        String traceId = resolveTraceId();
        log.info("Order creation initiated. customerId={}", customerId);
        Order savedOrder = null;
        OrderState fromState = OrderState.CREATED;
        OrderState toState   = OrderState.CREATED;
        try {
            //persist new order
            Order newOrder = new Order(customerId, orderRequest.totalAmount(), orderRequest.currency());
            savedOrder = orderRepository.save(newOrder);
            log.info("Order created successfully. orderId={}", savedOrder.getId());
            orderAuditService.recordOrderTransition(
                    savedOrder.getId(),
                    fromState,
                    toState,
                    OrderAction.PLACE_ORDER,
                    customerId.toString(),
                    traceId,
                    null
            );
            return buildOrderResponse(savedOrder);
        }
        catch (Exception ex) {
            log.error("Order creation failed for customerId={}, traceId={}", customerId, traceId, ex);
            if(savedOrder != null) {
                orderAuditService.recordOrderTransition(
                        savedOrder.getId(),
                        fromState,
                        toState,
                        OrderAction.PLACE_ORDER,
                        customerId.toString(),
                        traceId,
                        "FAILURE: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()
                );
            }
            throw ex;
        }
    }
    //Fetch order details
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        //Check order existence
        Order existingOrder = orderRepository.findByIdAndCustomerId(orderId, getCurrentCustomerId())
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        log.debug("Order fetched. orderId={}", existingOrder.getId());
        return buildOrderResponse(existingOrder);
    }
    //Cancel order
    @Transactional
    @Observed(name = "order.cancel")
    public OrderResponse cancelOrder(Long orderId) {
        //Check order existence
        Long customerId = getCurrentCustomerId();
        String traceId = resolveTraceId();
        Order existingOrder = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        log.info("Order cancellation initiated. orderId={}", existingOrder.getId());
        OrderState fromState = existingOrder.getOrderState();
        try {
            existingOrder.cancel();
            orderRepository.save(existingOrder);
            log.info("Order cancelled. orderId={}", existingOrder.getId());
            orderAuditService.recordOrderTransition(
                    existingOrder.getId(),
                    fromState,
                    existingOrder.getOrderState(),
                    OrderAction.USER_CANCEL_REQUEST,
                    customerId.toString(),
                    traceId,
                    null
            );
            return buildOrderResponse(existingOrder);
        }
        catch (Exception ex) {
            log.error("Order cancellation failed for orderId={}, customerId={}, traceId={}", existingOrder.getId(), customerId, traceId, ex);
            orderAuditService.recordOrderTransition(
                    existingOrder.getId(),
                    fromState,
                    fromState,
                    OrderAction.USER_CANCEL_REQUEST,
                    customerId.toString(),
                    traceId,
                    "FAILURE: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
            throw ex;
        }
    }
    /**
     * Retrieves a paginated list of orders for the current authenticated customer.
     * Implements Resource Cloaking to ensure data isolation.
     */
    @Transactional(readOnly = true)
    @Observed(name = "order.list.fetch")
    public PageOrderResponse getOrders(int page, int size, String orderState) {
        Long customerId = getCurrentCustomerId();

        // Defensive: Protect against "Query of Death" by capping the result set size
        if (size > MAX_PAGE_SIZE) {
            log.warn("Page size {} exceeds max limit. Capped to {} for customerId={}",
                    size, MAX_PAGE_SIZE, customerId);
        }

        int pageSize = Math.min(size, MAX_PAGE_SIZE);

        // Optimization: Use a stable sort key that matches our composite index
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());

        Page<Order> orderPage;

        // Branching Logic: Handle 'All' vs 'Filtered' views without defaulting to a single state
        if(orderState == null || orderState.isBlank()) {
            orderPage = orderRepository.findByCustomerId(customerId, pageable);
        }
        else {
            // Validation: Ensure the string maps to a valid domain Enum before hitting the DB
            OrderState currentOrderState = OrderState.fromString(orderState).orElseThrow(
                    () -> new InvalidRequestException(
                            String.format("Invalid order state: '%s'. Allowed values: %s",
                                    orderState,
                                    Arrays.toString(OrderState.values())
                            )));
            orderPage = orderRepository.findByCustomerIdAndOrderState(customerId, currentOrderState, pageable);
        }

        // Transformation: Convert Entity Page to DTO Page while preserving pagination metadata
        Page<OrderResponse> mappedPage = orderPage.map(this::buildOrderResponse);

        log.debug("Orders fetched. customerId={}, page={}, requestedSize={}, appliedSize={}, filter={} ", customerId, page, size, orderPage.getSize(), orderState);

        return new PageOrderResponse(
                    mappedPage.getContent(),
                    orderPage.getNumber(),
                    size,
                    orderPage.getSize(),
                    orderPage.getTotalElements(),
                    orderPage.getTotalPages(),
                    orderPage.isLast()
        );
    }
    private Long getCurrentCustomerId() {
        //Ownership check
        AuthenticatedUser user = RequestContext.get();
        if (user == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        return user.userId();
    }
    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) return traceId;
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                return currentSpan.context().traceId();
            }
        }
        return MDC.get("requestId");
    }
    private OrderResponse buildOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getOrderState(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}

