package com.esprit.reservation.mapper;

import com.esprit.reservation.dto.ReservationResponse;
import com.esprit.reservation.dto.ReservationStatusHistoryResponse;
import com.esprit.reservation.dto.CustomerResponse;
import com.esprit.reservation.entity.Reservation;
import com.esprit.reservation.entity.ReservationStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring", uses = {TableMapper.class})
public interface ReservationMapper {
    @Mapping(target = "customer", source = "reservation")
    ReservationResponse toResponse(Reservation reservation);
    
    List<ReservationResponse> toResponseList(List<Reservation> reservations);

    default CustomerResponse mapCustomer(Reservation reservation) {
        if (reservation == null) {
            return null;
        }
        return new CustomerResponse(
                null,
                reservation.getKeycloakUserId(),
                reservation.getCustomerName(),
                reservation.getCustomerEmail(),
                reservation.getCustomerPhone()
        );
    }
    
    ReservationStatusHistoryResponse toHistoryResponse(ReservationStatusHistory history);
}
