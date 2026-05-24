package com.esprit.reservation.service;

import com.esprit.reservation.entity.RestaurantRoom;
import com.esprit.reservation.repository.RestaurantRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RestaurantRoomRepository roomRepository;

    public List<RestaurantRoom> getAllRooms() {
        return roomRepository.findByActiveTrue();
    }

    public Optional<RestaurantRoom> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    @Transactional
    public RestaurantRoom createRoom(RestaurantRoom room) {
        room.setActive(true);
        return roomRepository.save(room);
    }
}
