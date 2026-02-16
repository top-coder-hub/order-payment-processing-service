/**
 * Created By Lavanyaa Karthik
 * Date: 09/01/26
 * Time: 10:20 pm
 */
package com.dev.order.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


@RestControllerAdvice
@Slf4j
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
    //Handle Validation / Request Errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing + " ; " + replacement
                ));
        log.warn("Field validation failed. errors={}", errors.keySet());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                HttpStatus.BAD_REQUEST.value(),
                "FIELD_VALIDATION_FAILED",
                "Request validation failed",
                errors,
                false,
                LocalDateTime.now(),
                resolveTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing + " ; " + replacement
                ));
        log.warn("Request body constraints violated. errors={}", errors.keySet());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_REQUEST",
                "Invalid request parameter(s)",
                errors,
                false,
                LocalDateTime.now(),
                resolveTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException ex) {
        log.warn("Missing request header. header={}", ex.getHeaderName());
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_REQUEST",
                "Required request header is missing",
                Map.of(ex.getHeaderName(),"Header is required"),
                false,
                LocalDateTime.now(),
                resolveTraceId()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request");
        return buildError(
                HttpStatus.BAD_REQUEST,
                "BAD_JSON",
                "Malformed or unreadable JSON request",
                false
        );
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("Http Request method not supported. method={}", ex.getMethod());
        return buildError(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "Requested method " + ex.getMethod() + " not allowed",
                false
        );
    }
    //Handle Business / Domain Exceptions
    @ExceptionHandler(BusinessRulesViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRulesViolation(BusinessRulesViolationException ex) {
        if(ex instanceof OrderContext orderContext) {
            log.warn("Business rule violated. errorCode={}, orderId={}, message={}",
                    ex.getErrCode(), orderContext.getOrderId(), ex.getMessage());
        }
        else if(ex instanceof PaymentContext paymentContext) {
            log.warn("Business rule violated. errorCode={}, paymentId={}, message={}",
                    ex.getErrCode(), paymentContext.getPaymentId(), ex.getMessage());
        }
        else {
            log.warn("Business rule violated. errorCode={}, message={}",
                    ex.getErrCode(), ex.getMessage());
        }
        return buildError(
                HttpStatus.CONFLICT,
                ex.getErrCode(),
                ex.getMessage(),
                false
        );
    }
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
        log.warn("Payment not found. paymentId={}", ex.getPaymentId());
        return buildError(
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND",
                ex.getMessage(),
                false
        );
    }
    //Authentication / Authorization Errors
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized request");
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                ex.getMessage(),
                false
        );
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access Denied");
        return buildError(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                ex.getMessage(),
                false
        );
    }
    //Handle missing Order
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found. orderId={}", ex.getOrderId());
        return buildError(
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND",
                ex.getMessage(),
                false
        );
    }
    //Handle client semantic error in request
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
        log.warn("Semantic validation failed. reason={}" , ex.getMessage());
        return buildError(
                HttpStatus.BAD_REQUEST,
                "INVALID_LOGICAL_REQUEST",
                ex.getMessage(),
                false
                );
    }
    //Infrastructure / Unexpected Exceptions
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(TimeoutException ex) {
        log.error("Gateway timeout occurred", ex);
        return buildError(
                HttpStatus.GATEWAY_TIMEOUT,
                "GATEWAY_TIMEOUT",
                "Request timed out",
                true
        );
    }
    @ExceptionHandler({
            DataAccessException.class,
            TransactionException.class,
            IOException.class,
            RuntimeException.class
    })
    public ResponseEntity<ErrorResponse> handleInfrastructureFailures(Exception ex) {
        log.error("Infrastructure failure", ex);
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "The service is temporarily unavailable",
                true
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
