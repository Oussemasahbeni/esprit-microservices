package com.esprit.reservation.repository;

import com.esprit.reservation.entity.Reservation;
import com.esprit.reservation.domain.ReservationCode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    @EntityGraph(attributePaths = {"table"})
    Optional<Reservation> findByReservationCode(ReservationCode reservationCode);

    @EntityGraph(attributePaths = {"table"})
    List<Reservation> findByKeycloakUserId(String keycloakUserId);

    @EntityGraph(attributePaths = {"table"})
    List<Reservation> findByCustomerEmail(com.esprit.reservation.domain.EmailAddress customerEmail);

    @EntityGraph(attributePaths = {"table"})
    List<Reservation> findByReservationDate(LocalDate reservationDate);

    @Query("SELECT r FROM Reservation r WHERE r.reservationDate = :date AND r.status IN (com.esprit.reservation.entity.ReservationStatus.CONFIRMED, com.esprit.reservation.entity.ReservationStatus.SEATED)")
    List<Reservation> findConfirmedAndSeatedReservationsOnDate(@Param("date") LocalDate date);

    @Query("SELECT r FROM Reservation r " +
           "WHERE r.reservationDate = :date " +
           "AND r.status IN (com.esprit.reservation.entity.ReservationStatus.CONFIRMED, com.esprit.reservation.entity.ReservationStatus.SEATED) " +
           "AND r.startTime < :endTime " +
           "AND r.endTime > :startTime")
    List<Reservation> findOverlappingReservations(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("SELECT r FROM Reservation r " +
           "WHERE r.table.id = :tableId " +
           "AND r.reservationDate = :date " +
           "AND r.status IN (com.esprit.reservation.entity.ReservationStatus.CONFIRMED, com.esprit.reservation.entity.ReservationStatus.SEATED) " +
           "AND r.startTime < :endTime " +
           "AND r.endTime > :startTime")
    List<Reservation> findOverlappingReservationsForTable(
            @Param("tableId") Long tableId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
