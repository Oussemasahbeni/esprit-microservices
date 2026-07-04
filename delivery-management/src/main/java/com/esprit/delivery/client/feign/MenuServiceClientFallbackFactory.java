package com.esprit.delivery.client.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback factory invoked by Resilience4j's Circuit Breaker when MS2 (Menu
 * Management Service) is unavailable (timeout, connection refused, open
 * circuit...). Lets the Delivery service fail fast with a clear, typed error
 * instead of letting the order-creation flow hang or throw an opaque error.
 *
 * Wired to {@code MenuServiceClient} through the {@code fallbackFactory}
 * attribute of {@code @FeignClient}, combined with the
 * {@code resilience4j.circuitbreaker} configuration declared in application.yml.
 */
@Slf4j
@Component
public class MenuServiceClientFallbackFactory implements FallbackFactory<MenuServiceClient> {

    @Override
    public MenuServiceClient create(Throwable cause) {
        log.warn("Menu Management Service unavailable, using fallback. Cause: {}", cause.getMessage());
        return dishId -> {
            throw new IllegalStateException(
                    "Menu service is currently unavailable, cannot validate dish " + dishId);
        };
    }
}
