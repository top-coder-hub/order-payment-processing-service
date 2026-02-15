/**
 * Created By Lavanyaa Karthik
 * Date: 12/01/26
 * Time: 5:18 am
 */
package com.dev.order.exception;

import lombok.Getter;

@Getter
public class OrderAmountMismatchException extends BusinessRulesViolationException {
    private final Long orderId;
    public OrderAmountMismatchException(Long orderId) {
        super("ORDER_AMOUNT_MISMATCH",
                "The requested payment amount does not match the calculated order amount.");
        this.orderId = orderId;
    }
}
