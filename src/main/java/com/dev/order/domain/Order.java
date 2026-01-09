/**
 * Created By Lavanyaa Karthik
 * Date: 01/01/26
 * Time: 10:55 pm
 */
package com.dev.order.domain;

import com.dev.order.domain.exception.InvalidOrderStateException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long customerId;
    private BigDecimal totalAmount;
    private String currency;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public void markAsPaid() {
        if(this.status != OrderStatus.CREATED) {
            throw new InvalidOrderStateException("ORDER.INVALID_STATE.PAYMENT", "Only CREATED orders can be marked as PAID.");
        }
        this.status = OrderStatus.PAID;
    }
    public void cancel() {
        if(this.status != OrderStatus.CREATED) {
            throw new InvalidOrderStateException("ORDER.INVALID_STATE.CANCELLATION", "Only CREATED orders can be CANCELLED.");
        }
        this.status = OrderStatus.CANCELLED;
    }
    public void markAsShipped() {
        if(this.status != OrderStatus.PAID) {
            throw new InvalidOrderStateException("ORDER.INVALID_STATE.SHIPPING", "Only PAID orders can be marked as SHIPPED.");
        }
        this.status = OrderStatus.SHIPPED;
    }
}
