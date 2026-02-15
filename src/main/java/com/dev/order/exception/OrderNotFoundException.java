/**
 * Created By Lavanyaa Karthik
 * Date: 12/01/26
 * Time: 4:47 am
 */
package com.dev.order.exception;

import lombok.Getter;

@Getter
public class OrderNotFoundException extends RuntimeException {
    private final Long orderId;
    public OrderNotFoundException(Long orderId) {
        super("The requested order was not found in the system.");
        this.orderId = orderId;
    }
}
