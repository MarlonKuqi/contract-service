package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.infrastructure.config.TimeZoneProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Converter(autoApply = true)
public class BusinessZoneDateTimeConverter implements AttributeConverter<LocalDateTime, OffsetDateTime> {

    private final ZoneId businessZone;

    public BusinessZoneDateTimeConverter(final TimeZoneProperties timeZoneProperties) {
        this.businessZone = timeZoneProperties.timeZone();
    }

    @Override
    public @Nullable OffsetDateTime convertToDatabaseColumn(final @Nullable LocalDateTime attribute) {
        return attribute == null ? null : attribute.atZone(businessZone).toOffsetDateTime();
    }

    @Override
    public @Nullable LocalDateTime convertToEntityAttribute(final @Nullable OffsetDateTime dbData) {
        return dbData == null ? null : dbData.atZoneSameInstant(businessZone).toLocalDateTime();
    }
}
