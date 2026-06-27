package com.esprit.employee.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishScheduleUpdated(LocalDate date, int availableStaff, String updatedBy) {
        ScheduleUpdatedEvent event = new ScheduleUpdatedEvent(
                UUID.randomUUID().toString(),
                date,
                availableStaff,
                updatedBy,
                LocalDateTime.now()
        );
        log.info("Publishing employee.schedule.updated for date {}: {} staff available", date, availableStaff);
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY_SCHEDULE_UPDATED, event);
    }
}
