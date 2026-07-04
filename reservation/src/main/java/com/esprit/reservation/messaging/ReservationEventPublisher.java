package com.esprit.reservation.messaging;

import com.esprit.reservation.config.RabbitMqConfig;
import com.esprit.reservation.entity.Reservation;
import com.esprit.reservation.entity.WaitlistEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishReservationConfirmed(Reservation reservation) {
        publish(reservation, RabbitMqConfig.ROUTING_KEY_CONFIRMED);
    }

    public void publishReservationCancelled(Reservation reservation) {
        publish(reservation, RabbitMqConfig.ROUTING_KEY_CANCELLED);
    }

    public void publishReservationWaitlisted(WaitlistEntry entry) {
        ReservationEvent event = new ReservationEvent(
                UUID.randomUUID().toString(),
                RabbitMqConfig.ROUTING_KEY_WAITLISTED,
                entry.getId(),
                null,
                entry.getCustomerEmail().value(),
                entry.getCustomerPhone() != null ? entry.getCustomerPhone().value() : null,
                entry.getRequestedDate(),
                entry.getRequestedTime(),
                entry.getGuestsCount().value(),
                null,
                LocalDateTime.now()
        );
        log.info("Publishing reservation.waitlisted for customer {}", entry.getCustomerEmail().value());
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY_WAITLISTED, event);
    }

    public void publishManagerAlert(ManagerAlertEvent alert) {
        log.warn("Publishing manager.staff.alert for date {} — {} reservations, {} staff available (need {})",
                alert.date(), alert.confirmedReservations(), alert.availableStaff(), alert.neededStaff());
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY_MANAGER_ALERT, alert);
    }

    private void publish(Reservation reservation, String routingKey) {
        ReservationEvent event = new ReservationEvent(
                UUID.randomUUID().toString(),
                routingKey,
                reservation.getId(),
                reservation.getReservationCode().value(),
                reservation.getCustomerEmail().value(),
                reservation.getCustomerPhone() != null ? reservation.getCustomerPhone().value() : null,
                reservation.getReservationDate(),
                reservation.getStartTime(),
                reservation.getGuestsCount().value(),
                reservation.getTable() != null ? reservation.getTable().getTableNumber() : null,
                LocalDateTime.now()
        );
        log.info("Publishing {} for reservation {}", routingKey, reservation.getReservationCode().value());
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, routingKey, event);
    }
}
