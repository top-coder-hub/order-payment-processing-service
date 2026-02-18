/**
 * Created By Lavanyaa Karthik
 * Date: 17/02/26
 * Time: 5:52 pm
 */
package com.dev.order.service;

import com.dev.order.domain.Order;
import com.dev.order.dto.CreateOrderRequest;
import com.dev.order.dto.OrderResponse;
import com.dev.order.exception.OrderNotFoundException;
import com.dev.order.exception.UnauthorizedException;
import com.dev.order.repository.OrderRepository;
import com.dev.order.security.AuthenticatedUser;
import com.dev.order.security.RequestContext;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
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
