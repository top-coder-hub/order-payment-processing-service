/**
 * Created By Lavanyaa Karthik
 * Date: 04/02/26
 * Time: 1:40 am
 */
package com.dev.order.config;

import com.dev.order.security.RequestContext;
import io.micrometer.tracing.Tracer;
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
    private final Tracer tracer;

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_KEY = "requestId";

    public FilterConfig(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Enforces a unique RequestId for every incoming request.
     * Priority: Ordered.HIGHEST_PRECEDENCE (Runs before security/controllers)
     */
    @Bean
    public FilterRegistrationBean<Filter> requestIdFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();

        registration.setFilter((request, response, chain) -> {
            //Defensive casting to ensure we are handling HTTP traffic
            if (!(request instanceof HttpServletRequest httpRequest) ||
                    !(response instanceof HttpServletResponse httpResponse)) {
                chain.doFilter(request, response);
                return;
            }

            //Logic: Reuse incoming ID (max 50 chars) or generate a new UUID
            String incomingId = httpRequest.getHeader(REQUEST_ID_HEADER);
            String requestId = (incomingId != null && !incomingId.isBlank() && incomingId.length() < 50)
                    ? incomingId
                    : UUID.randomUUID().toString();

            //Storage: Put in MDC for log correlation and add to Response Header
            MDC.put(MDC_KEY, requestId);
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

            //TraceId for logging and audit across services
            if (tracer != null) {
                var traceContext = tracer.currentTraceContext().context();
                if (traceContext != null) {
                    MDC.put("traceId", traceContext.traceId());
                }
            }


            try {
                //Continue the filter chain
                chain.doFilter(request, response);
            } finally {
                //Cleanup: Critical to prevent ThreadLocal memory leaks
                RequestContext.clear();
                MDC.clear(); //Clear ALL MDC keys at once (requestId, traceId, userId)
            }
        });

        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
