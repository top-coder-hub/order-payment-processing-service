/**
 * Created By Lavanyaa Karthik
 * Date: 04/02/26
 * Time: 1:40 am
 */
package com.dev.order.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.Filter;
import org.springframework.core.Ordered;

import java.util.UUID;

@Configuration
public class FilterConfig {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_KEY = "requestId";

    /**
     * Enforces a unique RequestId for every incoming request.
     * Priority: Ordered.HIGHEST_PRECEDENCE (Runs before security/controllers)
     */
    @Bean
    public FilterRegistrationBean<Filter> requestIdFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();

        registration.setFilter((request, response, chain) -> {
            // 1. Defensive casting to ensure we are handling HTTP traffic
            if (!(request instanceof HttpServletRequest httpRequest) ||
                    !(response instanceof HttpServletResponse httpResponse)) {
                chain.doFilter(request, response);
                return;
            }

            // 2. Logic: Reuse incoming ID (max 50 chars) or generate a new UUID
            String incomingId = httpRequest.getHeader(REQUEST_ID_HEADER);
            String requestId = (incomingId != null && !incomingId.isBlank() && incomingId.length() < 50)
                    ? incomingId
                    : UUID.randomUUID().toString();

            // 3. Storage: Put in MDC for log correlation and add to Response Header
            MDC.put(MDC_KEY, requestId);
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

            try {
                // 4. Continue the filter chain
                chain.doFilter(request, response);
            } finally {
                // 5. Cleanup: Critical to prevent ThreadLocal memory leaks
                MDC.remove(MDC_KEY);
            }
        });

        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
