/**
 * Created By Lavanyaa Karthik
 * Date: 01/01/26
 * Time: 10:58 pm
 */
package com.dev.order.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments", indexes = {
                @Index(name = "idx_payments_order", columnList = "order_id")
})
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
    public Order getOrder() {
        return order;
    }

    protected Payment() {
    }

    public Payment(Order order, BigDecimal amount, String currency, PaymentState paymentState, String idempotencyKey) {
        this.order = order;
        this.amount = amount;
        this.currency = currency;
        this.paymentState = paymentState;
        this.idempotencyKey = idempotencyKey;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getOrderId() {
        return order.getId();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentState getPaymentState() {
        return paymentState;
    }

    protected void setPaymentState(PaymentState paymentState) {
        this.paymentState = paymentState;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
