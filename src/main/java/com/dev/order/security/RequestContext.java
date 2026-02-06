/**
 * Created By Lavanyaa Karthik
 * Date: 06/02/26
 * Time: 11:17 pm
 */
package com.dev.order.security;

public class RequestContext {
    private static final ThreadLocal<AuthenticatedUser> CURRENT_USER = new ThreadLocal<>();

    private RequestContext() {}

    public static void set(AuthenticatedUser user) {
        CURRENT_USER.set(user);
    }

    public static AuthenticatedUser get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
