/**
 * Created By Lavanyaa Karthik
 * Date: 06/02/26
 * Time: 11:20 pm
 */
package com.dev.order.config;

import com.dev.order.exception.UnauthorizedException;
import com.dev.order.security.AuthenticatedUser;
import com.dev.order.security.RequestContext;
import com.dev.order.security.UserRole;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class AuthenticationFilterConfig {

    @Bean
    public FilterRegistrationBean<Filter> authenticationFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();

        registration.setFilter((request, response, chain) -> {

            if (!(request instanceof HttpServletRequest req) ||
                    !(response instanceof HttpServletResponse res)) {
                chain.doFilter(request, response);
                return;
            }

            try {
                String authHeader = req.getHeader("Authorization");

                // 1️⃣ Missing / invalid Authorization header
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw new UnauthorizedException("Missing or invalid authentication");
                }

                // 2️⃣ Extract token
                String token = authHeader.substring(7);

                // 3️⃣ Validate token (V1 stub)
                AuthenticatedUser user = validateToken(token);
                if (user == null) {
                    throw new UnauthorizedException("Missing or invalid authentication");
                }

                // 4️⃣ Store identity in request-scoped context
                RequestContext.set(user);

                // 5️⃣ Continue request
                chain.doFilter(request, response);

            } finally {
                // 6️⃣ Critical cleanup to avoid ThreadLocal leaks
                RequestContext.clear();
            }
        });

        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // runs after RequestId filter
        return registration;
    }

    /**
     * V1 token validation stub.
     * Real system would validate JWT / OAuth token.
     */
    private AuthenticatedUser validateToken(String token) {
        if ("customer-token".equals(token)) {
            return new AuthenticatedUser(1L, UserRole.CUSTOMER);
        }
        if ("admin-token".equals(token)) {
            return new AuthenticatedUser(2L, UserRole.ADMIN);
        }
        if ("system-token".equals(token)) {
            return new AuthenticatedUser(0L, UserRole.SYSTEM);
        }
        return null;
    }
}

