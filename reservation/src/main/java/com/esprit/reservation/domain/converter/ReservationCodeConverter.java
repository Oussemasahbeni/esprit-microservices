package com.esprit.reservation.domain.converter;

import com.esprit.reservation.domain.ReservationCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReservationCodeConverter implements AttributeConverter<ReservationCode, String> {
    @Override
    public String convertToDatabaseColumn(ReservationCode attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public ReservationCode convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ReservationCode.of(dbData);
    }
}
