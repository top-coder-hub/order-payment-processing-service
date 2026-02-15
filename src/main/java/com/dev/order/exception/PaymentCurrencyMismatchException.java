/**
 * Created By Lavanyaa Karthik
 * Date: 12/01/26
 * Time: 5:56 am
 */
package com.dev.order.exception;

import lombok.Getter;

@Getter
public class PaymentCurrencyMismatchException extends BusinessRulesViolationException {
    private final Long orderId;
    public PaymentCurrencyMismatchException(Long orderId) {
        super("ORDER_CURRENCY_MISMATCH",
                "Payment currency does not match order currency.");
        this.orderId = orderId;
    }
}
