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
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }
    @Transactional
    public PaymentResponse processPayment(Long orderId, PaymentRequest request, String idempotencyKey) {
        Optional<Payment> payment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        //Return the existing  payment status if payment already done
        if(payment.isPresent()) {
            Payment existingPayment = payment.get();
            return new PaymentResponse(
                    existingPayment.getPaymentId(),
                    existingPayment.getOrderId(),
                    existingPayment.getAmount(),
                    existingPayment.getPaymentState(),
                    existingPayment.getCreatedAt()
            );
        }
        //Check order existence
        Order existingOrder = orderRepository.findById(orderId).orElseThrow(
                () -> new OrderNotFoundException("ORDER_NOT_FOUND", "The requested order with ID " + orderId + " was not found in the system."));
        //Check if existing order status is in CREATED state
        if(existingOrder.getOrderState() != OrderState.CREATED) {
            throw new InvalidOrderStateException(
                    "INVALID_ORDER_STATE", "Cannot process payment on order " + orderId + ", because Order is currently not in 'CREATED' state.");
        }
        //Check if existing order amount matches the new payment request amount
        if(existingOrder.getTotalAmount().compareTo(request.amount()) != 0) {
            throw new OrderAmountMismatchException(
                    "ORDER_AMOUNT_MISMATCH", "The requested payment amount (" + request.amount() + "), does not match the calculated order amount (" + existingOrder.getTotalAmount() + ").");
        }
        //Check if existing order currency matches the new payment request currency
        if(!Objects.equals(existingOrder.getCurrency(), request.currency())) {
            throw new PaymentCurrencyMismatchException(
                    "ORDER_CURRENCY_MISMATCH", "The requested payment currency (" + request.currency() + "), does not match the order currency (" + existingOrder.getCurrency() + ").");
        }
        //persist new payment
        Payment newPayment = new Payment(existingOrder, request.amount(), existingOrder.getCurrency(), PaymentState.COMPLETED, idempotencyKey);
        Payment savedNewPayment = paymentRepository.save(newPayment);
        //Mark payment status of existing order as PAID & merge
        existingOrder.markAsPaid();
        Order savedExistingOrder = orderRepository.save(existingOrder);
        return new PaymentResponse(
                savedNewPayment.getPaymentId(),
                savedNewPayment.getOrderId(),
                savedNewPayment.getAmount(),
                savedNewPayment.getPaymentState(),
                savedNewPayment.getCreatedAt()
        );
    }
}
