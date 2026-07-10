package com.esprit.delivery.dto;

import com.esprit.delivery.entity.DeliveryAddress;
import java.util.List;
import lombok.*;

/**
 * Request payload for {@code POST /orders}.
 * Carries raw dish references; unit prices/names are resolved server-side
 * via {@code MenuServiceClient} to avoid trusting client-supplied prices.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    private String customerId;
    private List<OrderItemRequest> items;
    private DeliveryAddress deliveryAddress;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemRequest {
        private Long dishId;
        private Integer quantity;
        private String notes;
    }
}