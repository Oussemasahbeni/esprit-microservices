package com.esprit.delivery.messaging.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveredEvent {
    Long orderId;
    Long customerId;
    LocalDateTime deliveredAt;
}