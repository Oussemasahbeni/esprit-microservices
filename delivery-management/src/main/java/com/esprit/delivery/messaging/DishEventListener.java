package com.esprit.delivery.messaging;

import com.esprit.delivery.config.RabbitMQConfig;
import com.esprit.delivery.messaging.events.DishUnavailableEvent;
import com.esprit.delivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DishEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.DISH_UNAVAILABLE_QUEUE)
    public void onDishUnavailable(DishUnavailableEvent event) {
        log.info("Received dish-unavailable event for dishId={}", event.getDishId());
        try {
            orderService.handleDishUnavailable(event.getDishId());
        } catch (Exception e) {
            log.error("Failed processing dish-unavailable event for dishId={}", event.getDishId(), e);
            throw e; // rethrow so it goes to the DLQ instead of being silently acked
        }
    }
}