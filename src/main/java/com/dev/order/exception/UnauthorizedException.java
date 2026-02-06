/**
 * Created By Lavanyaa Karthik
 * Date: 06/02/26
 * Time: 11:32 pm
 */
package com.dev.order.exception;

public class UnauthorizedException extends RuntimeException{
    public UnauthorizedException(String message) {
        super(message);
    }
}
