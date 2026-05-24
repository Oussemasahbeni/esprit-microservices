package com.esprit.reservation.repository;

import com.esprit.reservation.entity.RestaurantTable;
import com.esprit.reservation.domain.GuestsCount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    
    @EntityGraph(attributePaths = {"room"})
    List<RestaurantTable> findByActiveTrue();

    @EntityGraph(attributePaths = {"room"})
    List<RestaurantTable> findByRoomIdAndActiveTrue(Long roomId);

    @EntityGraph(attributePaths = {"room"})
    List<RestaurantTable> findByCapacityGreaterThanEqualAndActiveTrueOrderByCapacityAsc(GuestsCount capacity);

    @EntityGraph(attributePaths = {"room"})
    Optional<RestaurantTable> findByTableNumberAndActiveTrue(String tableNumber);
}
