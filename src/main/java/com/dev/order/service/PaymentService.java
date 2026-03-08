/**
 * Created By Lavanyaa Karthik
 * Date: 12/01/26
 * Time: 4:13 am
 */
package com.dev.order.service;

import com.dev.order.audit.OrderAuditService;
import com.dev.order.audit.PaymentAuditService;
import com.dev.order.domain.*;
import com.dev.order.dto.PaymentRequest;
import com.dev.order.dto.PaymentResponse;
import com.dev.order.exception.*;
import com.dev.order.repository.OrderRepository;
import com.dev.order.repository.PaymentRepository;
import com.dev.order.security.AuthenticatedUser;
import com.dev.order.security.RequestContext;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentAuditService paymentAuditService;
    private final OrderAuditService orderAuditService;
    private final Tracer tracer;

    private final ObservationRegistry registry;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, PaymentAuditService paymentAuditService, OrderAuditService orderAuditService, Tracer tracer, ObservationRegistry registry) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentAuditService = paymentAuditService;
        this.orderAuditService = orderAuditService;
        this.tracer = tracer;
        this.registry = registry;
    }
    @Transactional
    @Observed(name = "payment.process")
    public PaymentResult processPayment(Long orderId, PaymentRequest request, String idempotencyKey) {
        Optional<Payment> payment = paymentRepository.findByIdempotencyKeyWithOrder(idempotencyKey);
        String traceId = resolveTraceId();
        Long customerId = getCurrentCustomerId();
        //1. Return the existing  payment status if payment already done
        if (payment.isPresent()) {
            return validateIdempotent(payment.get(), orderId, customerId, traceId);
        }
        //2. Defensive Ownership
        Order existingOrder = orderRepository.findByIdAndCustomerId(orderId, customerId).orElseThrow(
                () -> new OrderNotFoundException(orderId));

        //3. State Validation
        if (existingOrder.getOrderState() != OrderState.CREATED) {
            throw new InvalidOrderStateException(
                    "INVALID_ORDER_STATE", "Cannot process payment, because Order is currently not in 'CREATED' state.", orderId);
        }

        //4. Check if existing order amount matches the new payment request amount
        if (existingOrder.getTotalAmount().compareTo(request.amount()) != 0) {
            throw new OrderAmountMismatchException(orderId);
        }

        //5. Check if existing order currency matches the new payment request currency
        if (!Objects.equals(existingOrder.getCurrency(), request.currency())) {
            throw new PaymentCurrencyMismatchException(orderId);
        }

        //6. Orchestrated Execution with Observation
        Payment savedNewPayment = null;
        try {
            savedNewPayment = Observation.createNotStarted("payment.state.transition", registry)
                    .contextualName("process-payment-completion")
                    .highCardinalityKeyValue("order.id", orderId.toString())
                    .lowCardinalityKeyValue("status.target", "COMPLETED")
                    .observe(() -> {
                        log.info("Payment transition initiated. orderId={}", orderId);
                        Payment newPayment = new Payment(existingOrder, request.amount(), existingOrder.getCurrency(), idempotencyKey);
                        newPayment.markAsCompleted();
                        Payment saved = paymentRepository.save(newPayment);
                        paymentAuditService.recordPaymentTransition(
                                saved.getPaymentId(),
                                PaymentState.PENDING,
                                saved.getPaymentState(),
                                PaymentAction.PAYMENT_SUCCESS,
                                saved.getOrder().getCustomerId().toString(),
                                traceId,
                                null
                        );
                        //Dynamically inject the DB-generated ID into the trace
                        if (registry.getCurrentObservation() != null) {
                            registry.getCurrentObservation().highCardinalityKeyValue("payment.id", saved.getPaymentId().toString());
                        }

                        log.info("Payment transition completed. paymentId={}, orderId={}", saved.getPaymentId(), orderId);
                        OrderState  fromState = existingOrder.getOrderState();
                        existingOrder.markAsPaid();
                        Order savedOrder = orderRepository.save(existingOrder);
                        log.info("Order marked as PAID orderId={}", orderId);
                        orderAuditService.recordOrderTransition(
                                savedOrder.getId(),
                                fromState,
                                savedOrder.getOrderState(),
                                OrderAction.PAYMENT_SUCCESS,
                                customerId.toString(),
                                traceId,
                                null
                        );

                        return saved; // Return the saved entity from the lambda
                    });

            return new PaymentResult(buildPaymentResponse(savedNewPayment), true);
        }
        catch (Exception ex) {
            log.error("Payment process failed for orderId={}, customerId={}, traceId={}", orderId, customerId, traceId, ex);
            if (savedNewPayment != null) {
                paymentAuditService.recordPaymentTransition(
                        savedNewPayment.getPaymentId(),
                        PaymentState.PENDING,
                        PaymentState.PENDING,
                        PaymentAction.PAYMENT_FAILURE,
                        savedNewPayment.getOrder().getCustomerId().toString(),
                        traceId,
                        "FAILURE: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()
                );
            }
            throw ex;
        }
    }
    @Transactional(readOnly = true)
    public PaymentResult fetchPayment(Long paymentId) {
        // Fetch payment or cloak as 404
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId));

        // Ownership verification only. Fetch owning order or cloak as 404 (defensive)
        Order order = orderRepository.findByIdAndCustomerId(payment.getOrderId(), getCurrentCustomerId())
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId));
        PaymentResponse paymentResponse = buildPaymentResponse(payment);
        log.debug("Payment fetched. paymentId={}, payment_state={}",
                payment.getPaymentId(),
                payment.getPaymentState());
        return new PaymentResult(paymentResponse, false);
    }
    private PaymentResult validateIdempotent(Payment existingPayment, Long orderId, Long customerId, String traceId) {
        PaymentState fromState = existingPayment.getPaymentState();
        try {
            //check if idempotencyKey belongs to the given orderId
            if(!existingPayment.getOrderId().equals(orderId)) {
                throw new OrderNotFoundException(orderId);
            }
            // Verify ownership via the associated order
            if (!existingPayment.getOrder().getCustomerId().equals(customerId)) {
                throw new OrderNotFoundException(orderId);
            }
            log.info("Idempotency replay detected for orderId={}", orderId);
            paymentAuditService.recordPaymentTransition(
                    existingPayment.getPaymentId(),
                    fromState,
                    fromState,
                    PaymentAction.REPLAY_DETECTED,
                    existingPayment.getOrder().getCustomerId().toString(),
                    traceId,
                    null
            );
            return new PaymentResult(buildPaymentResponse(existingPayment), false);
        }
        catch (Exception ex) {
            log.error("Payment idempotent replay failed for paymentId={}, customerId={}, traceId={}", existingPayment.getPaymentId(), customerId, traceId, ex);
            paymentAuditService.recordPaymentTransition(
                    existingPayment.getPaymentId(),
                    fromState,
                    fromState,
                    PaymentAction.REPLAY_DETECTED,
                    existingPayment.getOrder().getCustomerId().toString(),
                    traceId,
                    "FAILURE: " + ex.getClass().getSimpleName() + ": " + ex.getMessage()
            );
            throw ex;
        }
    }
    private Long getCurrentCustomerId() {
        // Ownership check (cloaked)
        AuthenticatedUser user = RequestContext.get();
        if (user == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        return user.userId();
    }
    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) return traceId;
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                return currentSpan.context().traceId();
            }
        }
        return MDC.get("requestId");
    }
    private PaymentResponse buildPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getPaymentState(),
                payment.getCreatedAt()
        );
    }
}
