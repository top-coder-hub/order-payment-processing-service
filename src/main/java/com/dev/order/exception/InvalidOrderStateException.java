/**
 * Created By Lavanyaa Karthik
 * Date: 09/01/26
 * Time: 10:17 pm
 */
package com.dev.order.exception;

public class InvalidOrderStateException extends BusinessRulesViolationException{
    public InvalidOrderStateException(String errCode, String reason) {
        super(409, errCode, reason);
    }
}
