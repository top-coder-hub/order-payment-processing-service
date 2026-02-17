/**
 * Created By Lavanyaa Karthik
 * Date: 01/01/26
 * Time: 10:58 pm
 */
package com.dev.order.domain;

import com.dev.order.exception.InvalidPaymentStateException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments", indexes = {
                @Index(name = "idx_payments_order", columnList = "order_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;
    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;
    @Column(nullable = false, length = 3, updatable = false)
    private String currency;
    @Column(name = "payment_state", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentState paymentState;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    public Payment(Order order, BigDecimal amount, String currency, String idempotencyKey) {
        this.order = order;
        this.amount = amount;
        this.currency = currency;
        this.paymentState = PaymentState.PENDING;
        this.idempotencyKey = idempotencyKey;
    }
    public Long getOrderId() {
        return order.getId();
    }
    public void markAsCompleted() {
        if(this.paymentState != PaymentState.PENDING) {
            throw new InvalidPaymentStateException("PAYMENT.INVALID_STATE.COMPLETION", "Only PENDING payments can be completed", getPaymentId());
        }
        this.paymentState = PaymentState.COMPLETED;
    }
    public void markAsFailed() {
        if(this.paymentState != PaymentState.PENDING) {
            throw new InvalidPaymentStateException("PAYMENT.INVALID_STATE.FAILURE", "Only PENDING payments can fail", getPaymentId());
        }
        this.paymentState = PaymentState.FAILED;
    }
}
