package com.esprit.gateway.routes;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.filter.CircuitBreakerFilterFunctions.circuitBreaker;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {
        return route("employee-management")
                .route(path("/api/employees/**", "/employee-management/v3/api-docs"), http())
                .before(rewritePath("/employee-management/v3/api-docs", "/v3/api-docs"))
                .filter(lb("employee-management"))
                .filter(circuitBreaker("employee-management", URI.create("forward:/fallback/employee-management")))
                .build()

                .and(route("menu-management")
                        .route(path("/api/menus/**", "/menu-management/v3/api-docs"), http())
                        .before(rewritePath("/menu-management/v3/api-docs", "/v3/api-docs"))
                        .filter(lb("menu-management"))
                        .filter(circuitBreaker("menu-management", URI.create("forward:/fallback/menu-management")))
                        .build())

                .and(route("delivery-management")
                        .route(path("/api/deliveries/**", "/delivery-management/v3/api-docs"), http())
                        .before(rewritePath("/delivery-management/v3/api-docs", "/v3/api-docs"))
                        .filter(lb("delivery-management"))
                        .filter(circuitBreaker("delivery-management", URI.create("forward:/fallback/delivery-management")))
                        .build())

                .and(route("reservation")
                        .route(path(
                                "/api/reservations/**",
                                "/api/availability/**",
                                "/api/tables/**",
                                "/api/rooms/**",
                                "/api/manager/**",
                                "/reservation/v3/api-docs"), http())
                        .before(rewritePath("/reservation/v3/api-docs", "/v3/api-docs"))
                        .filter(lb("reservation"))
                        .filter(circuitBreaker("reservation", URI.create("forward:/fallback/reservation")))
                        .build());
    }
}