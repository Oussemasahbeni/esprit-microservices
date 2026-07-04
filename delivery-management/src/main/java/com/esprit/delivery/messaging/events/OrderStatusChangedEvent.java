package com.esprit.delivery.messaging.events;

import com.esprit.delivery.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangedEvent {
    Long orderId;
    Long customerId;
    OrderStatus previousStatus;
    OrderStatus newStatus;
}