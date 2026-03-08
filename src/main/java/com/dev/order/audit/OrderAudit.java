/**
 * Created By Lavanyaa Karthik
 * Date: 28/02/26
 * Time: 11:07 pm
 */
package com.dev.order.audit;

import com.dev.order.domain.OrderAction;
import com.dev.order.domain.OrderState;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table (
        name = "order_audit",
        indexes = {
            @Index(name = "idx_order_audit_created_desc", columnList = "created_at"),
            @Index(name = "idx_order_audit_order_created_desc", columnList = "order_id,created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", nullable = false, length = 50)
    private OrderState fromState;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 50)
    private OrderState toState;
    @Enumerated(EnumType.STRING)
    @Column(name = "order_action", nullable = false, length = 50)
    private OrderAction orderAction;
    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;
    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public OrderAudit(Long orderId, OrderState fromState, OrderState toState, OrderAction orderAction, String changedBy, String traceId, String errorMessage) {
        this.orderId = orderId;
        this.fromState = fromState;
        this.toState = toState;
        this.orderAction = orderAction;
        this.changedBy = changedBy;
        this.traceId = traceId;
        this.errorMessage = errorMessage;
    }
    public static OrderAudit record(
            Long orderId,
            OrderState fromState,
            OrderState toState,
            OrderAction orderAction,
            String changedBy,
            String traceId,
            String errorMessage
    ) {
        return new OrderAudit(orderId, fromState, toState, orderAction, changedBy, traceId, errorMessage);
    }
}
