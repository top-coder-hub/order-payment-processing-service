/**
 * Created By Lavanyaa Karthik
 * Date: 12/01/26
 * Time: 4:47 am
 */
package com.dev.order.exception;

public class OrderNotFoundException extends RuntimeException{
    public OrderNotFoundException(String reason) {
        super(reason);
    }
}
