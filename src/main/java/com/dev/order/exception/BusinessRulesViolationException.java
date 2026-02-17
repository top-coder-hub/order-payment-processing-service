/**
 * Created By Lavanyaa Karthik
 * Date: 09/01/26
 * Time: 10:18 pm
 */
package com.dev.order.exception;

import lombok.Getter;

@Getter
public class BusinessRulesViolationException extends RuntimeException {
    private final String errCode;
    public BusinessRulesViolationException(String errCode, String reason) {
        super(reason);
        this.errCode = errCode;
    }
}
