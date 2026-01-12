/**
 * Created By Lavanyaa Karthik
 * Date: 12/01/26
 * Time: 5:18 am
 */
package com.dev.order.exception;

public class OrderAmountMismatchException extends BusinessRulesViolationException{
    public OrderAmountMismatchException(String errCode, String reason) {
        super(409, errCode, reason);
    }
}
