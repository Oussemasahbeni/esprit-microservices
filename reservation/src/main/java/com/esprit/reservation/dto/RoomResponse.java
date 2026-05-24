package com.esprit.reservation.dto;

public record RoomResponse(
    Long id,
    String name,
    String description,
    Integer floorNumber,
    Boolean active
) {}
