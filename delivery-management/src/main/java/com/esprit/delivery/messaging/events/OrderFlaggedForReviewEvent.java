package com.esprit.delivery.messaging.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFlaggedForReviewEvent {
    Long orderId;
    Long customerId;
    Long dishId;
    String reason;
}