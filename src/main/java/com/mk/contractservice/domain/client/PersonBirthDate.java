package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.client.exception.InvalidPersonBirthDateException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonBirthDate {

    LocalDate value;

    public static PersonBirthDate of(@Nullable final LocalDate value, final LocalDate today) {
        notNull(today);
        return ValueObjectUtils.validateAndCreate(
                value,
                PersonBirthDate::normalize,
                normalized -> validate(normalized, today),
                PersonBirthDate::new
        );
    }

    public static PersonBirthDate reconstituteFromDatabase(final LocalDate trustedValue) {
        return new PersonBirthDate(notNull(trustedValue));
    }

    private static LocalDate normalize(@Nullable final LocalDate rawValue) {
        if (rawValue == null) {
            throw new InvalidPersonBirthDateException("Birth date must not be null");
        }
        return rawValue;
    }

    private static void validate(final LocalDate normalized, final LocalDate today) {
        if (normalized.isAfter(today)) {
            throw new InvalidPersonBirthDateException("Birth date cannot be in the future");
        }
    }
}
