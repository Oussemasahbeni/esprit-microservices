package com.esprit.reservation.repository;

import com.esprit.reservation.entity.ReservationPreOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationPreOrderItemRepository extends JpaRepository<ReservationPreOrderItem, Long> {

    @Query("SELECT i FROM ReservationPreOrderItem i " +
           "WHERE i.menuDishId = :dishId " +
           "AND i.reservation.status IN (com.esprit.reservation.entity.ReservationStatus.CONFIRMED, com.esprit.reservation.entity.ReservationStatus.SEATED) " +
           "AND i.reservation.reservationDate >= CURRENT_DATE")
    List<ReservationPreOrderItem> findUpcomingByDishId(@Param("dishId") Long dishId);
}
