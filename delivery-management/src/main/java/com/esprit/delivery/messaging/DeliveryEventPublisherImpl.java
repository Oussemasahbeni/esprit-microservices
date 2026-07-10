package com.esprit.delivery.messaging;

import com.esprit.delivery.config.RabbitMQConfig;
import com.esprit.delivery.messaging.events.OrderDeliveredEvent;
import com.esprit.delivery.messaging.events.OrderFlaggedForReviewEvent;
import com.esprit.delivery.messaging.events.OrderPlacedEvent;
import com.esprit.delivery.messaging.events.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventPublisherImpl implements DeliveryEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishOrderPlaced(OrderPlacedEvent event) {
        send(RabbitMQConfig.ROUTING_ORDER_PLACED, event);
    }

    @Override
    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        send(RabbitMQConfig.ROUTING_ORDER_STATUS_CHANGED, event);
    }

    @Override
    public void publishOrderDelivered(OrderDeliveredEvent event) {
        send(RabbitMQConfig.ROUTING_ORDER_DELIVERED, event);
    }

    @Override
    public void publishOrderFlaggedForReview(OrderFlaggedForReviewEvent event) {
        send(RabbitMQConfig.ROUTING_ORDER_FLAGGED, event);
    }

    private void send(String routingKey, Object payload) {
        try {
            log.info("Sending ");
            rabbitTemplate.convertAndSend(RabbitMQConfig.DELIVERY_EXCHANGE, routingKey, payload);
        } catch (Exception e) {
            // Don't let a broker hiccup roll back the DB transaction that already committed.
            // Log + consider an outbox pattern if you need guaranteed delivery.
            log.error("Failed to publish event with routing key {}: {}", routingKey, e.getMessage(), e);
        }
    }
}