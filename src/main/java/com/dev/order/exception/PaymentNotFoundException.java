/**
 * Created By Lavanyaa Karthik
 * Date: 08/02/26
 * Time: 12:10 am
 */
package com.dev.order.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
