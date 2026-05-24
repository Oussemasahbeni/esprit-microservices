package com.esprit.reservation.mapper;

import com.esprit.reservation.dto.WaitlistEntryResponse;
import com.esprit.reservation.dto.CustomerResponse;
import com.esprit.reservation.entity.WaitlistEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface WaitlistMapper {
    @Mapping(target = "customer", source = "entry")
    WaitlistEntryResponse toResponse(WaitlistEntry entry);
    
    List<WaitlistEntryResponse> toResponseList(List<WaitlistEntry> entries);

    default CustomerResponse mapCustomer(WaitlistEntry entry) {
        if (entry == null) {
            return null;
        }
        return new CustomerResponse(
                null,
                entry.getKeycloakUserId(),
                entry.getCustomerName(),
                entry.getCustomerEmail(),
                entry.getCustomerPhone()
        );
    }
}
