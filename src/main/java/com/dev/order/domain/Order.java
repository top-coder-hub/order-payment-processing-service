/**
 * Created By Lavanyaa Karthik
 * Date: 01/01/26
 * Time: 10:55 pm
 */
package com.dev.order.domain;

import com.dev.order.exception.InvalidOrderStateException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public Order(Long customerId, BigDecimal totalAmount, String currency) {
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.orderState = OrderState.CREATED;
    }

    public void markAsPaid() {
        if(this.orderState != OrderState.CREATED) {
            throw new InvalidOrderStateException("ORDER.INVALID_STATE.PAYMENT", "Only CREATED orders can be marked as PAID.", getId());
        }
        this.orderState = OrderState.PAID;
    }
    public void cancel() {
        if(this.orderState != OrderState.CREATED) {
            throw new InvalidOrderStateException("ORDER.INVALID_STATE.CANCELLATION", "Only CREATED orders can be CANCELLED.", getId());
        }
        this.orderState = OrderState.CANCELLED;
    }
    public void markAsShipped() {
        if(this.orderState != OrderState.PAID) {
            throw new InvalidOrderStateException("ORDER.INVALID_STATE.SHIPPING", "Only PAID orders can be marked as SHIPPED.", getId());
        }
        this.orderState = OrderState.SHIPPED;
    }
}
