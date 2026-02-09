/**
 * Created By Lavanyaa Karthik
 * Date: 12/01/26
 * Time: 5:56 am
 */
package com.dev.order.exception;

public class PaymentCurrencyMismatchException extends BusinessRulesViolationException{
    public PaymentCurrencyMismatchException(String errCode, String reason) {
        super(errCode, reason);
    }
}
