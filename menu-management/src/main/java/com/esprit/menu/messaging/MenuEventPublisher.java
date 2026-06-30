package com.esprit.menu.messaging;

import com.esprit.menu.entity.Dish;
import com.esprit.menu.entity.Promotion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MenuEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishDishUnavailable(Dish dish) {
        var event = new DishUnavailableEvent(
                UUID.randomUUID().toString(),
                RabbitMqConfig.ROUTING_KEY_DISH_UNAVAILABLE,
                dish.getId(),
                dish.getName(),
                dish.getCategory().getId(),
                dish.getCategory().getName(),
                LocalDateTime.now()
        );
        log.info("Publishing dish.unavailable for dish {}", dish.getId());
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY_DISH_UNAVAILABLE, event);
    }

    public void publishDishCreated(Dish dish) {
        publishDishSync(dish, RabbitMqConfig.ROUTING_KEY_DISH_CREATED);
    }

    public void publishDishUpdated(Dish dish) {
        publishDishSync(dish, RabbitMqConfig.ROUTING_KEY_DISH_UPDATED);
    }

    public void publishDishDeleted(Dish dish) {
        var event = new DishSyncEvent(
                UUID.randomUUID().toString(),
                RabbitMqConfig.ROUTING_KEY_DISH_DELETED,
                dish.getId(),
                dish.getName(),
                dish.getPrice(),
                false,
                dish.getCategory().getId(),
                dish.getCategory().getName(),
                LocalDateTime.now()
        );
        log.info("Publishing dish.deleted for dish {}", dish.getId());
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY_DISH_DELETED, event);
    }

    private void publishDishSync(Dish dish, String routingKey) {
        var event = new DishSyncEvent(
                UUID.randomUUID().toString(),
                routingKey,
                dish.getId(),
                dish.getName(),
                dish.getPrice(),
                dish.isAvailable(),
                dish.getCategory().getId(),
                dish.getCategory().getName(),
                LocalDateTime.now()
        );
        log.info("Publishing {} for dish {}", routingKey, dish.getId());
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, routingKey, event);
    }

    public void publishPromotionCreated(Promotion promotion) {
        var event = new PromotionCreatedEvent(
                UUID.randomUUID().toString(),
                RabbitMqConfig.ROUTING_KEY_PROMO_CREATED,
                promotion.getId(),
                promotion.getName(),
                promotion.getDiscountPercent(),
                promotion.getDish() != null ? promotion.getDish().getId() : null,
                promotion.getCategory() != null ? promotion.getCategory().getId() : null,
                promotion.getStartsAt(),
                promotion.getEndsAt(),
                LocalDateTime.now()
        );
        log.info("Publishing promo.created for promotion {}", promotion.getId());
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY_PROMO_CREATED, event);
    }
}
