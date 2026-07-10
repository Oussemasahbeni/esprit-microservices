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
  String customerId;
  Long dishId;
  String reason;
}
