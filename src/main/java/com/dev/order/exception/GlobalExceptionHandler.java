/**
 * Created By Lavanyaa Karthik
 * Date: 09/01/26
 * Time: 10:20 pm
 */
package com.dev.order.exception;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.time.LocalDateTime;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {
    private record ErrorResponse (
            boolean success,
            Integer status,
            String errorCode,
            String reason,
            Map<String, String> errors,
            boolean retryable,
            LocalDateTime timestamp,
            String traceId
    ) {}
    @ExceptionHandler(BusinessRulesViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRulesViolation(BusinessRulesViolationException ex) {
        return buildError(
                HttpStatus.CONFLICT,
                ex.errCode,
                ex.getMessage(),
                false
        );
    }
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                ex.getMessage(),
                false
        );
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildError(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                ex.getMessage(),
                false
        );
    }
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND",
                ex.getMessage(),
                false
        );
    }
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND",
                ex.getMessage(),
                false
        );
    }
    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return MDC.get("requestId");
    }
    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String errorCode, String reason, boolean retryable) {
        ErrorResponse error = new ErrorResponse(
                false,
                status.value(),
                errorCode,
                reason,
                null,
                retryable,
                LocalDateTime.now(),
                resolveTraceId()
        );
        return ResponseEntity.status(status).body(error);
    }
}
