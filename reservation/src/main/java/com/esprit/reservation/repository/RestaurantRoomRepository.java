package com.esprit.reservation.repository;

import com.esprit.reservation.entity.RestaurantRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RestaurantRoomRepository extends JpaRepository<RestaurantRoom, Long> {
    List<RestaurantRoom> findByActiveTrue();
}
