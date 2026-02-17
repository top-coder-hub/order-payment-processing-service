/**
 * Created By Lavanyaa Karthik
 * Date: 07/02/26
 * Time: 1:11 am
 */
package com.dev.order.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
