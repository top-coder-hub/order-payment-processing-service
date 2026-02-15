/**
 * Created By Lavanyaa Karthik
 * Date: 08/02/26
 * Time: 12:10 am
 */
package com.dev.order.exception;

import lombok.Getter;

@Getter
public class PaymentNotFoundException extends RuntimeException {
    private final Long paymentId;
    public PaymentNotFoundException(Long paymentId) {
        super("Payment not found for the given identifier.");
        this.paymentId = paymentId;
    }
}
