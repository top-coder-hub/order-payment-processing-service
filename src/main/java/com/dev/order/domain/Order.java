/**
 * Created By Lavanyaa Karthik
 * Date: 01/01/26
 * Time: 10:55 pm
 */
package com.dev.order.domain;

import com.dev.order.exception.InvalidOrderStateException;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(
        name = "orders", indexes = {
                @Index(name = "idx_orders_customer", columnList = "customer_id"),
                @Index(name = "idx_orders_customer_state", columnList = "customer_id,order_state"), //Composite Index
                @Index(name = "idx_orders_created", columnList = "created_at")
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "order_state", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderState orderState;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Order() {
    }

    public Order(Long customerId, BigDecimal totalAmount, String currency, OrderState orderState) {
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.orderState = orderState;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public OrderState getOrderState() {
        return orderState;
    }

    protected void setOrderState(OrderState orderState) {
        this.orderState = orderState;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markAsPaid() {
        if(this.orderState != OrderState.CREATED) {
            throw new InvalidOrderStateException("ORDER.INVALID_STATE.PAYMENT", "Only CREATED orders can be marked as PAID.");
        }
        this.orderState = OrderState.PAID;
    }
    public void cancel() {
        if(this.orderState != OrderState.CREATED) {
            throw new InvalidOrderStateException("ORDER.INVALID_STATE.CANCELLATION", "Only CREATED orders can be CANCELLED.");
        }
        this.orderState = OrderState.CANCELLED;
    }
    public void markAsShipped() {
        if(this.orderState != OrderState.PAID) {
            throw new InvalidOrderStateException("ORDER.INVALID_STATE.SHIPPING", "Only PAID orders can be marked as SHIPPED.");
        }
        this.orderState = OrderState.SHIPPED;
    }
}
