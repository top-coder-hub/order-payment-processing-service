/**
 * Created By Lavanyaa Karthik
 * Date: 03/02/26
 * Time: 1:32 am
 */
package com.dev.order.controller;

import com.dev.order.dto.PaymentRequest;
import com.dev.order.dto.PaymentResponse;
import com.dev.order.exception.AccessDeniedException;
import com.dev.order.exception.UnauthorizedException;
import com.dev.order.security.AuthenticatedUser;
import com.dev.order.security.RequestContext;
import com.dev.order.security.UserRole;
import com.dev.order.service.PaymentResult;
import com.dev.order.service.PaymentService;
import io.micrometer.tracing.Tracer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1")
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    //create payment
    @PostMapping("/orders/{orderId}/payments")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Idempotency-Key must follow UUID format") String idempotencyKey,
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest paymentRequest) {
        //Authorization check
        authorize();
        //Entry log (Logging the Order ID)
        log.info("Create payment request received for orderId={}", orderId);
        PaymentResult paymentResult = paymentService.processPayment(orderId, paymentRequest, idempotencyKey);
        if(paymentResult.isNewlyCreated()) {
            // Exit Log (Success)
            log.info("Payment created successfully. paymentId={}", paymentResult.paymentResponse().paymentId());
            return ResponseEntity.status(HttpStatus.CREATED).body(paymentResult.paymentResponse());
        }
        // Exit Log (Success)
        log.info("Idempotency replay detected. Returning existing payment. paymentId={}", paymentResult.paymentResponse().paymentId());
        return ResponseEntity.status(HttpStatus.OK).body(paymentResult.paymentResponse());
    }
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        //Authorization check
        authorize();
        //Entry log (Logging the Order ID)
        log.info("Get payment request received for paymentId={}", paymentId);
        PaymentResult paymentResult = paymentService.fetchPayment(paymentId);
        // Exit Log (Success)
        log.info("Payment fetched successfully for paymentId={}", paymentId);
        return ResponseEntity.ok().body(paymentResult.paymentResponse());
    }
    private void authorize() {
        AuthenticatedUser user = RequestContext.get();
        if(user == null) {
            throw new UnauthorizedException("Unauthenticated request");
        }
        if(user.role() == UserRole.SYSTEM) {
            throw new AccessDeniedException("SYSTEM role is not allowed for public APIs");
        }
    }
}
