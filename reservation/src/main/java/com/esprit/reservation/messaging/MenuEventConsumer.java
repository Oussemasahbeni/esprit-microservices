package com.esprit.reservation.messaging;

import com.esprit.reservation.config.RabbitMqConfig;
import com.esprit.reservation.entity.ReservationPreOrderItem;
import com.esprit.reservation.repository.ReservationPreOrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consumes menu-management's dish events to flag pre-ordered dishes that became unavailable
 * after a reservation was made. No menu data is cached here — the booking flow's available
 * dishes/promotions are fetched live from menu-management (see MenuSnapshotService).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MenuEventConsumer {

    private final ReservationPreOrderItemRepository preOrderItemRepository;

    @Transactional
    @RabbitListener(queues = RabbitMqConfig.DISH_SYNC_QUEUE)
    public void onDishSync(DishSyncEvent event) {
        boolean dishGone = "dish.deleted".equals(event.eventType()) || !event.available();
        if (!dishGone) {
            return;
        }

        log.info("Dish {} ({}) is no longer available — flagging affected pre-orders",
                event.dishId(), event.dishName());
        flagPreOrdersAsUnavailable(event.dishId());
    }

    private void flagPreOrdersAsUnavailable(Long dishId) {
        List<ReservationPreOrderItem> affected = preOrderItemRepository.findUpcomingByDishId(dishId);
        affected.forEach(item -> item.setStillAvailable(false));
        preOrderItemRepository.saveAll(affected);
    }
}
