/**
 * Created By Lavanyaa Karthik
 * Date: 18/02/26
 * Time: 2:48 am
 */
package com.dev.order.controller;

import com.dev.order.dto.CreateOrderRequest;
import com.dev.order.dto.OrderResponse;
import com.dev.order.dto.PageOrderResponse;
import com.dev.order.exception.AccessDeniedException;
import com.dev.order.exception.UnauthorizedException;
import com.dev.order.security.AuthenticatedUser;
import com.dev.order.security.RequestContext;
import com.dev.order.security.UserRole;
import com.dev.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1")
@Slf4j
public class OrderController {
    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest orderRequest) {
        authorize();
        log.info("Create order request received.");
        OrderResponse orderResponse = orderService.createOrder(orderRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable @Positive Long orderId) {
        authorize();
        log.info("Get order request received. orderId={}", orderId);
        OrderResponse orderResponse = orderService.getOrderById(orderId);
        return ResponseEntity.status(HttpStatus.OK).body(orderResponse);
    }
    @GetMapping("/orders")
    public ResponseEntity<PageOrderResponse> getOrders(
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) Integer size,
            @RequestParam(required = false) String orderState) {
        authorize();
        log.debug("Get orders request received. page={}, size={}, orderState={}", page, size, orderState);
        PageOrderResponse pageOrderResponse = orderService.getOrders(page, size, orderState);
        return ResponseEntity.ok(pageOrderResponse);
    }
    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable @Positive Long orderId) {
        authorize();
        log.info("Cancel order request received. orderId={}", orderId);
        OrderResponse orderResponse = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(orderResponse);
    }
    private void authorize() {
        AuthenticatedUser user = RequestContext.get();
        if(user == null) {
            throw new UnauthorizedException("Unauthenticated request");
        }
        if(user.role() == UserRole.SYSTEM) {
            throw new AccessDeniedException("SYSTEM role is not allowed for public APIs");
        }
    }
}
