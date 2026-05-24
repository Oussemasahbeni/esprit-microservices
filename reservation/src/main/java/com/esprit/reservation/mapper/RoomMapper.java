package com.esprit.reservation.mapper;

import com.esprit.reservation.dto.RoomResponse;
import com.esprit.reservation.entity.RestaurantRoom;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    RoomResponse toResponse(RestaurantRoom room);
    List<RoomResponse> toResponseList(List<RestaurantRoom> rooms);
}
