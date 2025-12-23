package com.mk.contractservice.domain.client.valueobject;

import com.mk.contractservice.domain.client.exception.InvalidPersonBirthDateException;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Predicate;

public final class PersonBirthDate {

    public static final Predicate<LocalDate> IS_IN_FUTURE =
            date -> date.isAfter(LocalDate.now());

    private final LocalDate value;

    private PersonBirthDate(final LocalDate value) {
        this.value = value;
    }

    public static PersonBirthDate of(@Nullable final LocalDate value) {
        validate(value);
        return new PersonBirthDate(value);
    }

    private static void validate(@Nullable final LocalDate birthDate) {
        if (birthDate == null) {
            throw new InvalidPersonBirthDateException("Birth date must not be null");
        }
        if (IS_IN_FUTURE.test(birthDate)) {
            throw new InvalidPersonBirthDateException("Birth date cannot be in the future");
        }
    }

    public LocalDate value() {
        return value;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        return this == o || (o instanceof PersonBirthDate other && Objects.equals(value, other.value));
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
