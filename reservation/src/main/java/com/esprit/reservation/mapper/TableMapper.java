package com.esprit.reservation.mapper;

import com.esprit.reservation.dto.TableResponse;
import com.esprit.reservation.entity.RestaurantTable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TableMapper {
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "xPosition", source = "XPosition")
    @Mapping(target = "yPosition", source = "YPosition")
    TableResponse toResponse(RestaurantTable table);
    List<TableResponse> toResponseList(List<RestaurantTable> tables);
}
