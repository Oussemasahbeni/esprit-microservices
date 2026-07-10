package com.esprit.delivery.messaging;

import com.esprit.delivery.messaging.events.OrderDeliveredEvent;
import com.esprit.delivery.messaging.events.OrderFlaggedForReviewEvent;
import com.esprit.delivery.messaging.events.OrderPlacedEvent;
import com.esprit.delivery.messaging.events.OrderStatusChangedEvent;

public interface DeliveryEventPublisher {
    void publishOrderPlaced(OrderPlacedEvent event);

    void publishOrderStatusChanged(OrderStatusChangedEvent event);

    void publishOrderDelivered(OrderDeliveredEvent event);

    void publishOrderFlaggedForReview(OrderFlaggedForReviewEvent event);
}