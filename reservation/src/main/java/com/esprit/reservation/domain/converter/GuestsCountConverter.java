package com.esprit.reservation.domain.converter;

import com.esprit.reservation.domain.GuestsCount;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GuestsCountConverter implements AttributeConverter<GuestsCount, Integer> {
    @Override
    public Integer convertToDatabaseColumn(GuestsCount attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public GuestsCount convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : GuestsCount.of(dbData);
    }
}
