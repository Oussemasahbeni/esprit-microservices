package com.esprit.delivery.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Forwards the caller's "Authorization" header on outgoing Feign calls to MS1.
 * <p>
 * MS1's {@code GET /api/employees/{id}} is {@code @PreAuthorize("hasAnyRole('admin', 'manager')")},
 * so calls made without a token are rejected with 401 - this makes the delivery
 * service call MS1 "as" the currently authenticated user instead of anonymously.
 * <p>
 * NOTE: this only works for calls made within an incoming HTTP request that
 * already carries a bearer token (e.g. an admin hitting
 * {@code POST /drivers/register} directly). It will NOT work for calls
 * triggered outside a request context (scheduled jobs, async/@Async methods,
 * message-listener-driven code) since there's no HttpServletRequest to read
 * the header from there - see Option B below for that case.
 * <p>
 * Not annotated with {@code @Configuration} on purpose, so it isn't picked up
 * globally by component scanning - it's wired in only via
 * {@code @FeignClient(configuration = FeignClientAuthConfig.class)}.
 */
public class FeignClientAuthConfig {

    @Bean
    public RequestInterceptor bearerTokenForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            String authorization = request.getHeader("Authorization");
            if (authorization != null) {
                template.header("Authorization", authorization);
            }
        };
    }
}