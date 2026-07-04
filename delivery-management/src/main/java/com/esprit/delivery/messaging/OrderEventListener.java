package com.esprit.delivery.messaging;

import com.esprit.delivery.config.RabbitMQConfig;
import com.esprit.delivery.messaging.events.OrderDeliveredEvent;
import com.esprit.delivery.messaging.events.OrderFlaggedForReviewEvent;
import com.esprit.delivery.messaging.events.OrderPlacedEvent;
import com.esprit.delivery.messaging.events.OrderStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitMQConfig.ORDER_EVENTS_QUEUE)
@Slf4j
public class OrderEventListener {

    @RabbitHandler
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Order placed: orderId={}, customerId={}, total={}",
                event.getOrderId(), event.getCustomerId(), event.getTotalAmount());
        // TODO: hook up whatever should react to a new order (e.g. notify customer, kick off prep tracking)
    }

    @RabbitHandler
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Order {} status changed: {} -> {}",
                event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());
        // TODO: hook up status-change side effects (e.g. push notification to customer)
    }

    @RabbitHandler
    public void onOrderDelivered(OrderDeliveredEvent event) {
        log.info("Order {} delivered at {}", event.getOrderId(), event.getDeliveredAt());
        // TODO: hook up post-delivery logic (e.g. trigger review request, loyalty points)
    }

    @RabbitHandler
    public void onOrderFlaggedForReview(OrderFlaggedForReviewEvent event) {
        log.warn("Order {} flagged for review: dishId={}, reason={}",
                event.getOrderId(), event.getDishId(), event.getReason());
        // TODO: hook up review workflow (e.g. notify support team, auto-suggest substitute dish)
    }

    @RabbitHandler(isDefault = true)
    public void onUnknownEvent(Object event) {
        log.warn("Received unrecognized order event type: {}", event.getClass().getName());
    }
}