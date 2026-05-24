package com.esprit.reservation.controller;

import com.esprit.reservation.dto.RoomResponse;
import com.esprit.reservation.entity.RestaurantRoom;
import com.esprit.reservation.mapper.RoomMapper;
import com.esprit.reservation.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getRooms() {
        return ResponseEntity.ok(roomMapper.toResponseList(roomService.getAllRooms()));
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@RequestBody RestaurantRoom room) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomMapper.toResponse(roomService.createRoom(room)));
    }
}
