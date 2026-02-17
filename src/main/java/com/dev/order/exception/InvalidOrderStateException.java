/**
 * Created By Lavanyaa Karthik
 * Date: 09/01/26
 * Time: 10:17 pm
 */
package com.dev.order.exception;

import lombok.Getter;

@Getter

public class InvalidOrderStateException extends BusinessRulesViolationException implements OrderContext {
    private final Long orderId;
    public InvalidOrderStateException(String errCode, String reason, Long orderId) {
        super(errCode, reason);
        this.orderId = orderId;
    }
}
