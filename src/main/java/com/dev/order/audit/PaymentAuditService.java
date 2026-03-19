/**
 * Created By Lavanyaa Karthik
 * Date: 07/03/26
 * Time: 2:31 am
 */
package com.dev.order.audit;

import com.dev.order.domain.PaymentAction;
import com.dev.order.domain.PaymentState;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAuditService {
    private final PaymentAuditRepository paymentAuditRepository;
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPaymentTransition(
            Long paymentId,
            PaymentState fromState,
            PaymentState toState,
            PaymentAction paymentAction,
            String changedBy,
            String traceId,
            String errorMessage
    ) {
        try {
            PaymentAudit paymentAudit = PaymentAudit.record(
                    paymentId,
                    fromState,
                    toState,
                    paymentAction,
                    changedBy,
                    traceId,
                    errorMessage
            );
            paymentAuditRepository.save(paymentAudit);
        }
        catch (Exception ex) {
            log.error("CRITICAL: Audit persistence failed for paymentId={}, action={}, traceId={}", paymentId, paymentAction, traceId, ex);
        }
    }
}
