/**
 * Created By Lavanyaa Karthik
 * Date: 09/01/26
 * Time: 10:20 pm
 */
package com.dev.order.domain.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private record ErrorResponse (
            LocalDateTime timestamp,
            boolean success,
            Integer errStatus,
            String errCode,
            Map<String, String> errors
    ) {}
    @ExceptionHandler(BusinessRulesViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRulesViolation(BusinessRulesViolationException e) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                false,
                e.errStatus,
                e.errCode,
                Map.of("error", e.getMessage())
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }
}
