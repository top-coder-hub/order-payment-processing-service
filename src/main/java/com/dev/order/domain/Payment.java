/**
 * Created By Lavanyaa Karthik
 * Date: 01/01/26
 * Time: 10:58 pm
 */
package com.dev.order.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments", indexes = {
                @Index(name = "idx_order", columnList = "order_id"),
                @Index(name = "idx_idempotency", columnList = "idempotency_key")
})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false) @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)

    private String idempotencyKey;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    public Order getOrder() {
        return order;
    }

    protected Payment() {
    }

    public Payment(Order order, BigDecimal amount, String currency, PaymentStatus status) {
        this.order = order;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
