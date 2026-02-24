/**
 * Created By Lavanyaa Karthik
 * Date: 12/01/26
 * Time: 4:13 am
 */
package com.dev.order.service;

import com.dev.order.domain.Order;
import com.dev.order.domain.OrderState;
import com.dev.order.domain.Payment;
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
import org.springframework.beans.factory.annotation.Autowired;
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

    private final ObservationRegistry registry;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, ObservationRegistry registry) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.registry = registry;
    }
    @Transactional
    @Observed(name = "payment.process")
    public PaymentResult processPayment(Long orderId, PaymentRequest request, String idempotencyKey) {
        Optional<Payment> payment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        //1. Return the existing  payment status if payment already done
        if(payment.isPresent()) {
            Payment existingPayment = payment.get();
            //check if idempotencyKey belongs to the given orderId
            if(!existingPayment.getOrderId().equals(orderId)) {
                throw new OrderNotFoundException(orderId);
            }
            // Verify ownership via the associated order
            if (!existingPayment.getOrder().getCustomerId().equals(getCurrentCustomerId())) {
                throw new OrderNotFoundException(orderId);
            }
            log.info("Idempotency replay detected for orderId={}", orderId);
            return new PaymentResult(buildPaymentResponse(existingPayment), false);
        }

        //2. Defensive Ownership
        Order existingOrder = orderRepository.findByIdAndCustomerId(orderId, getCurrentCustomerId()).orElseThrow(
                () -> new OrderNotFoundException(orderId));

        //3. State Validation
        if(existingOrder.getOrderState() != OrderState.CREATED) {
            throw new InvalidOrderStateException(
                    "INVALID_ORDER_STATE", "Cannot process payment, because Order is currently not in 'CREATED' state.", orderId);
        }

        //4. Check if existing order amount matches the new payment request amount
        if(existingOrder.getTotalAmount().compareTo(request.amount()) != 0) {
            throw new OrderAmountMismatchException(orderId);
        }

        //5. Check if existing order currency matches the new payment request currency
        if(!Objects.equals(existingOrder.getCurrency(), request.currency())) {
            throw new PaymentCurrencyMismatchException(orderId);
        }

        //6. Orchestrated Execution with Observation
        final Payment savedNewPayment = Observation.createNotStarted("payment.state.transition", registry)
                .contextualName("process-payment-completion")
                .highCardinalityKeyValue("order.id", orderId.toString())
                .lowCardinalityKeyValue("status.target", "COMPLETED")
                .observe(() -> {
                    log.info("Payment transition initiated. orderId={}", orderId);
                    Payment newPayment = new Payment(existingOrder, request.amount(), existingOrder.getCurrency(), idempotencyKey);
                    newPayment.markAsCompleted();
                    Payment saved = paymentRepository.save(newPayment);

                    //Dynamically inject the DB-generated ID into the trace
                    if(registry.getCurrentObservation() != null) {
                        registry.getCurrentObservation().highCardinalityKeyValue("payment.id", saved.getPaymentId().toString());
                    }

                    log.info("Payment transition completed. paymentId={}, orderId={}", saved.getPaymentId(), orderId);

                    existingOrder.markAsPaid();
                    orderRepository.save(existingOrder);
                    log.info("Order marked as PAID orderId={}", orderId);

                    return saved; // Return the saved entity from the lambda
                });

        return new PaymentResult(buildPaymentResponse(savedNewPayment), true);
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
    private Long getCurrentCustomerId() {
        // Ownership check (cloaked)
        AuthenticatedUser user = RequestContext.get();
        if (user == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        return user.userId();
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
