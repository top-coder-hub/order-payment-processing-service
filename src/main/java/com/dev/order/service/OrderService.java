/**
 * Created By Lavanyaa Karthik
 * Date: 17/02/26
 * Time: 5:52 pm
 */
package com.dev.order.service;

import com.dev.order.domain.Order;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private static final int MAX_PAGE_SIZE = 100;
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    //Create order
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest orderRequest) {
        Long customerId = getCurrentCustomerId();
        log.info("Order creation initiated.");
        //persist new order
        Order newOrder = new Order(customerId, orderRequest.totalAmount(), orderRequest.currency());
        Order savedOrder = orderRepository.save(newOrder);
        log.info("Order created successfully. orderId={}", savedOrder.getId());
        return buildOrderResponse(savedOrder);
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
    public OrderResponse cancelOrder(Long orderId) {
        //Check order existence
        Order existingOrder = orderRepository.findByIdAndCustomerId(orderId, getCurrentCustomerId())
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        log.info("Order cancellation initiated. orderId={}", existingOrder.getId());
        existingOrder.cancel();
        log.info("Order cancelled. orderId={}", existingOrder.getId());
        return buildOrderResponse(existingOrder);
    }
    /**
     * Retrieves a paginated list of orders for the current authenticated customer.
     * Implements Resource Cloaking to ensure data isolation.
     */
    @Transactional(readOnly = true)
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
