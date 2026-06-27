package com.esprit.employee.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduleEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ScheduleEventPublisher publisher;

    @Test
    void publishScheduleUpdated_sendsToCorrectExchangeAndRoutingKey() {
        LocalDate date = LocalDate.of(2026, 5, 25);

        publisher.publishScheduleUpdated(date, 3, "keycloak-user-123");

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.EXCHANGE),
                eq(RabbitMqConfig.ROUTING_KEY_SCHEDULE_UPDATED),
                org.mockito.ArgumentMatchers.any(ScheduleUpdatedEvent.class)
        );
    }

    @Test
    void publishScheduleUpdated_eventContainsCorrectPayload() {
        LocalDate date = LocalDate.of(2026, 5, 25);
        ArgumentCaptor<ScheduleUpdatedEvent> captor = ArgumentCaptor.forClass(ScheduleUpdatedEvent.class);

        publisher.publishScheduleUpdated(date, 2, "keycloak-user-456");

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.EXCHANGE),
                eq(RabbitMqConfig.ROUTING_KEY_SCHEDULE_UPDATED),
                captor.capture()
        );

        ScheduleUpdatedEvent event = captor.getValue();
        assertThat(event.date()).isEqualTo(date);
        assertThat(event.availableStaff()).isEqualTo(2);
        assertThat(event.updatedBy()).isEqualTo("keycloak-user-456");
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void publishScheduleUpdated_withZeroStaff_stillPublishes() {
        LocalDate date = LocalDate.of(2026, 6, 1);
        ArgumentCaptor<ScheduleUpdatedEvent> captor = ArgumentCaptor.forClass(ScheduleUpdatedEvent.class);

        publisher.publishScheduleUpdated(date, 0, "keycloak-user-789");

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.EXCHANGE),
                eq(RabbitMqConfig.ROUTING_KEY_SCHEDULE_UPDATED),
                captor.capture()
        );
        assertThat(captor.getValue().availableStaff()).isZero();
    }
}
