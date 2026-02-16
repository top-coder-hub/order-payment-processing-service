/**
 * Created By Lavanyaa Karthik
 * Date: 16/02/26
 * Time: 11:25 pm
 */
package com.dev.order.exception;

import lombok.Getter;

@Getter
public class InvalidPaymentStateException extends BusinessRulesViolationException implements PaymentContext {
    private final Long paymentId;
    public InvalidPaymentStateException(String errCode, String reason, Long paymentId) {
        super(errCode, reason);
        this.paymentId = paymentId;
    }
}
