/**
 * Created By Lavanyaa Karthik
 * Date: 05/03/26
 * Time: 2:20 am
 */
package com.dev.order.audit;

import com.dev.order.domain.OrderAction;
import com.dev.order.domain.OrderState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAuditService {
    private final OrderAuditRepository orderAuditRepository;
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOrderTransition(
            Long orderId,
            OrderState fromState,
            OrderState toState,
            OrderAction orderAction,
            String changedBy,
            String traceId,
            String errorMessage
    ) {
        try {
            OrderAudit orderAudit = OrderAudit.record(
                    orderId,
                    fromState,
                    toState,
                    orderAction,
                    changedBy,
                    traceId,
                    errorMessage
            );
            orderAuditRepository.save(orderAudit);
        }
        catch (Exception ex) {
            log.error("CRITICAL: Audit persistence failed for orderId={}, action={}, traceId={}", orderId, orderAction, traceId, ex);
        }
    }
}
