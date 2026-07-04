package com.esprit.delivery.client.feign;

import com.esprit.delivery.client.feign.dto.DishResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Synchronous client used to call MS2 (Menu Management Service) directly,
 * through the Spring Cloud Gateway and resolved dynamically via Consul
 * (service discovery). Declared with {@code name} matching the service's
 * registration name in Consul; no hardcoded URL is needed.
 * <p>
 * Used by {@code OrderService#placeOrder} to fetch authoritative dish
 * name/price and availability at order-creation time, instead of trusting
 * client-supplied values.
 */
@FeignClient(name = "menu-management", path = "/api/menus")
public interface MenuServiceClient {

    @GetMapping("/dishes/{id}")
    DishResponse getDishById(@PathVariable("id") Long dishId);
}
