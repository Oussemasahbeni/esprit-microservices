package com.esprit.reservation.messaging;

import com.esprit.reservation.config.RabbitMqConfig;
import com.esprit.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleUpdatedConsumer {

    private static final int RESERVATIONS_PER_STAFF = 5;

    private final ReservationRepository reservationRepository;
    private final ReservationEventPublisher eventPublisher;

    @RabbitListener(queues = RabbitMqConfig.SCHEDULE_UPDATES_QUEUE)
    public void onScheduleUpdated(ScheduleUpdatedEvent event) {
        log.info("Received employee.schedule.updated for date {}: {} staff available",
                event.date(), event.availableStaff());

        int confirmedCount = reservationRepository
                .findConfirmedAndSeatedReservationsOnDate(event.date())
                .size();

        int neededStaff = Math.max(1, (int) Math.ceil(confirmedCount / (double) RESERVATIONS_PER_STAFF));

        if (event.availableStaff() < neededStaff) {
            String message = String.format(
                    "%d reservations on %s but only %d staff scheduled — need at least %d",
                    confirmedCount, event.date(), event.availableStaff(), neededStaff
            );

            ManagerAlertEvent alert = new ManagerAlertEvent(
                    UUID.randomUUID().toString(),
                    "STAFF_SHORTAGE",
                    event.date(),
                    confirmedCount,
                    event.availableStaff(),
                    neededStaff,
                    message,
                    LocalDateTime.now()
            );

            eventPublisher.publishManagerAlert(alert);
        }
    }
}
