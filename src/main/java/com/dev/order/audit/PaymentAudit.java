/**
 * Created By Lavanyaa Karthik
 * Date: 28/02/26
 * Time: 11:07 pm
 */
package com.dev.order.audit;

import com.dev.order.domain.OrderAction;
import com.dev.order.domain.OrderState;
import com.dev.order.domain.PaymentAction;
import com.dev.order.domain.PaymentState;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table (
        name = "payment_audit",
        indexes = {
                @Index(name = "idx_payment_audit_created_desc", columnList = "created_at"),
                @Index(name = "idx_payment_audit_payment_created_desc", columnList = "payment_id,created_at")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", nullable = false, length = 50)
    private PaymentState fromState;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 50)
    private PaymentState toState;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_action", nullable = false, length = 50)
    private PaymentAction paymentAction;
    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;
    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PaymentAudit(Long paymentId, PaymentState fromState, PaymentState toState, PaymentAction paymentAction, String changedBy, String traceId, String errorMessage) {
        this.paymentId = paymentId;
        this.fromState = fromState;
        this.toState = toState;
        this.paymentAction = paymentAction;
        this.changedBy = changedBy;
        this.traceId = traceId;
        this.errorMessage = errorMessage;
    }
    public static PaymentAudit record(
            Long paymentId,
            PaymentState fromState,
            PaymentState toState,
            PaymentAction paymentAction,
            String changedBy,
            String traceId,
            String errorMessage
    ) {
        return new PaymentAudit(paymentId, fromState, toState, paymentAction, changedBy, traceId, errorMessage);
    }
}
