/**
 * Created By Lavanyaa Karthik
 * Date: 10/02/26
 * Time: 5:45 am
 */
package com.dev.order.exception;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
