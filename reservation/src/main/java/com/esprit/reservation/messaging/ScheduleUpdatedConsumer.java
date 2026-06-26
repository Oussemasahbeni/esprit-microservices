package com.esprit.reservation.messaging;

import com.esprit.reservation.config.RabbitMqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScheduleUpdatedConsumer {

    @RabbitListener(queues = RabbitMqConfig.SCHEDULE_UPDATES_QUEUE)
    public void onScheduleUpdated(ScheduleUpdatedEvent event) {
        log.info("Received employee.schedule.updated for date {}: {} staff available",
                event.date(), event.availableStaff());

        if (event.availableStaff() < 1) {
            log.warn("STAFF ALERT: No staff scheduled for {} — manager must review reservations for that day",
                    event.date());
        }
    }
}
