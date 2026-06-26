package com.esprit.reservation.messaging;

import com.esprit.reservation.config.RabbitMqConfig;
import com.esprit.reservation.domain.EmailAddress;
import com.esprit.reservation.domain.GuestsCount;
import com.esprit.reservation.domain.PhoneNumber;
import com.esprit.reservation.domain.ReservationCode;
import com.esprit.reservation.entity.Reservation;
import com.esprit.reservation.entity.ReservationStatus;
import com.esprit.reservation.entity.RestaurantTable;
import com.esprit.reservation.entity.TableStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ReservationEventPublisher publisher;

    @Test
    void publishReservationConfirmed_sendsToCorrectExchangeAndRoutingKey() {
        Reservation reservation = buildReservation();

        publisher.publishReservationConfirmed(reservation);

        ArgumentCaptor<ReservationEvent> captor = ArgumentCaptor.forClass(ReservationEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.EXCHANGE),
                eq(RabbitMqConfig.ROUTING_KEY_CONFIRMED),
                captor.capture()
        );
        ReservationEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(RabbitMqConfig.ROUTING_KEY_CONFIRMED);
        assertThat(event.reservationId()).isEqualTo(1L);
        assertThat(event.reservationCode()).isEqualTo("RES-20260525-ABCDEF12");
        assertThat(event.customerEmail()).isEqualTo("john@example.com");
        assertThat(event.guestsCount()).isEqualTo(4);
        assertThat(event.tableNumber()).isEqualTo("T01");
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void publishReservationCancelled_sendsToCorrectExchangeAndRoutingKey() {
        Reservation reservation = buildReservation();

        publisher.publishReservationCancelled(reservation);

        ArgumentCaptor<ReservationEvent> captor = ArgumentCaptor.forClass(ReservationEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.EXCHANGE),
                eq(RabbitMqConfig.ROUTING_KEY_CANCELLED),
                captor.capture()
        );
        assertThat(captor.getValue().eventType()).isEqualTo(RabbitMqConfig.ROUTING_KEY_CANCELLED);
    }

    private Reservation buildReservation() {
        RestaurantTable table = RestaurantTable.builder()
                .id(1L)
                .tableNumber("T01")
                .capacity(4)
                .status(TableStatus.AVAILABLE)
                .build();

        return Reservation.builder()
                .id(1L)
                .reservationCode(ReservationCode.of("RES-20260525-ABCDEF12"))
                .customerEmail(EmailAddress.of("john@example.com"))
                .customerPhone(PhoneNumber.of("+21622111222"))
                .reservationDate(LocalDate.of(2026, 5, 25))
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(22, 0))
                .guestsCount(GuestsCount.of(4))
                .table(table)
                .status(ReservationStatus.CONFIRMED)
                .build();
    }
}
