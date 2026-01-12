/**
 * Created By Lavanyaa Karthik
 * Date: 12/01/26
 * Time: 4:47 am
 */
package com.dev.order.exception;

public class OrderNotFoundException extends BusinessRulesViolationException{
    public OrderNotFoundException(String errCode, String reason) {
        super(404, errCode, reason);
    }
}
