/**
 * Created By Lavanyaa Karthik
 * Date: 06/02/26
 * Time: 11:17 pm
 */
package com.dev.order.security;
import org.slf4j.MDC;

public class RequestContext {
    private static final ThreadLocal<AuthenticatedUser> CURRENT_USER = new ThreadLocal<>();

    private RequestContext() {}

    public static void set(AuthenticatedUser user) {
        if (user != null) {
            CURRENT_USER.set(user);
            MDC.put("userId", String.valueOf(user.userId()));
        }
    }

    public static AuthenticatedUser get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
