/**
 * Created By Lavanyaa Karthik
 * Date: 12/01/26
 * Time: 4:13 am
 */
package com.dev.order.service;

import com.dev.order.domain.Order;
import com.dev.order.domain.OrderState;
import com.dev.order.domain.Payment;
import com.dev.order.domain.PaymentState;
import com.dev.order.dto.PaymentRequest;
import com.dev.order.dto.PaymentResponse;
import com.dev.order.exception.*;
import com.dev.order.repository.OrderRepository;
import com.dev.order.repository.PaymentRepository;
import com.dev.order.security.AuthenticatedUser;
import com.dev.order.security.RequestContext;
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

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }
    @Transactional
    public PaymentResult processPayment(Long orderId, PaymentRequest request, String idempotencyKey) {
        Optional<Payment> payment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        //Return the existing  payment status if payment already done
        if(payment.isPresent()) {
            Payment existingPayment = payment.get();
            //check if idempotencyKey belongs to the given orderId
            if(!existingPayment.getOrderId().equals(orderId)) {
                throw new OrderNotFoundException(orderId);
            }
            orderRepository.findByIdAndCustomerId(orderId, getCurrentCustomerId()).orElseThrow(
                    () -> new OrderNotFoundException(orderId));
            PaymentResponse existingPaymentResponse = buildPaymentResponse(existingPayment);
            log.debug("Idempotency replay detected for orderId={}", orderId);
            return new PaymentResult(existingPaymentResponse, false);
        }

        //Check order existence or cloak as 404 (defensive)
        Order existingOrder = orderRepository.findByIdAndCustomerId(orderId, getCurrentCustomerId()).orElseThrow(
                () -> new OrderNotFoundException(orderId));

        //Check if existing order status is in CREATED state
        if(existingOrder.getOrderState() != OrderState.CREATED) {
            throw new InvalidOrderStateException(
                    "INVALID_ORDER_STATE", "Cannot process payment, because Order is currently not in 'CREATED' state.", orderId);
        }

        //Check if existing order amount matches the new payment request amount
        if(existingOrder.getTotalAmount().compareTo(request.amount()) != 0) {
            throw new OrderAmountMismatchException(orderId);
        }

        //Check if existing order currency matches the new payment request currency
        if(!Objects.equals(existingOrder.getCurrency(), request.currency())) {
            throw new PaymentCurrencyMismatchException(orderId);
        }

        //persist new payment
        log.info("Payment initiated. orderId={}", orderId);
        Payment newPayment = new Payment(existingOrder, request.amount(), existingOrder.getCurrency(), idempotencyKey);
        newPayment.markAsCompleted();
        Payment savedNewPayment = paymentRepository.save(newPayment);
        log.info("Payment completed. paymentId={}, orderId={}", savedNewPayment.getPaymentId(), orderId);

        // Transition order state: CREATED → PAID
        existingOrder.markAsPaid();
        log.info("Order marked as PAID orderId={}", orderId);
        PaymentResponse newPaymentResponse = buildPaymentResponse(savedNewPayment);
        return new PaymentResult(newPaymentResponse, true);
    }
    @Transactional(readOnly = true)
    public PaymentResult fetchPayment(Long paymentId) {
        // Fetch payment or cloak as 404
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId));

        // Fetch owning order or cloak as 404 (defensive)
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
