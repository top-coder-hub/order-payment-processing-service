/**
 * Created By Lavanyaa Karthik
 * Date: 09/01/26
 * Time: 10:18 pm
 */
package com.dev.order.exception;

import jakarta.persistence.Id;

public class BusinessRulesViolationException extends RuntimeException {
    Integer errStatus;
    String errCode;
    String reason;
    public BusinessRulesViolationException(Integer errStatus, String errCode, String reason) {
        super(reason);
        this.errStatus = errStatus;
        this.errCode = errCode;
    }
}
