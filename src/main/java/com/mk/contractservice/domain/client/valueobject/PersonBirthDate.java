package com.mk.contractservice.domain.client.valueobject;

import com.mk.contractservice.domain.client.exception.InvalidPersonBirthDateException;
import com.mk.contractservice.domain.shared.ValueObject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.function.Predicate;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonBirthDate {

    LocalDate value;

    public static final Predicate<LocalDate> IS_IN_FUTURE =
            date -> date.isAfter(LocalDate.now());

    public static PersonBirthDate of(@Nullable final LocalDate value) {
        return ValueObject.validateAndCreate(
                value,
                PersonBirthDate::normalize,
                PersonBirthDate::validate,
                PersonBirthDate::new
        );
    }

    public static PersonBirthDate reconstituteFromDatabase(final LocalDate trustedValue) {
        return ValueObject.guardNotNull(trustedValue, PersonBirthDate::new, PersonBirthDate.class);
    }

    private static LocalDate normalize(@Nullable final LocalDate rawValue) {
        if (rawValue == null) {
            throw new InvalidPersonBirthDateException("Birth date must not be null");
        }
        return rawValue;
    }

    private static void validate(final LocalDate normalized) {
        if (IS_IN_FUTURE.test(normalized)) {
            throw new InvalidPersonBirthDateException("Birth date cannot be in the future");
        }
    }
}
