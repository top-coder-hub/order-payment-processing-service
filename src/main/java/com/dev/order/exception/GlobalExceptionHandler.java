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
    public ResponseEntity<ErrorResponse> handleBusinessRulesViolation(BusinessRulesViolationException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                false,
                e.errStatus,
                e.errCode,
                e.reason,
                Map.of("error", e.getMessage()),
                false,
                LocalDateTime.now(),
                resolveTraceId()
        );
        return ResponseEntity.status(e.errStatus).body(errorResponse);
    }
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        ErrorResponse error = new ErrorResponse(
                false,
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                ex.getMessage(),
                null,
                false,
                LocalDateTime.now(),
                resolveTraceId()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(
                false,
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                ex.getMessage(),
                null,
                false,
                LocalDateTime.now(),
                resolveTraceId()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return MDC.get("requestId");
    }

}
