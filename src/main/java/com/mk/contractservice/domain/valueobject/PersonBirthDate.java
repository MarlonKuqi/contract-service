package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class PersonBirthDate {

    @Column(name = "birth_date", nullable = false, updatable = false)
    private final LocalDate value;

    private PersonBirthDate(final LocalDate value) {
        this.value = value;
    }

    public static PersonBirthDate of(final LocalDate value) {
        validate(value);
        return new PersonBirthDate(value);
    }

    private static void validate(final LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date must not be null");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future");
        }
    }

    @JsonValue
    public LocalDate value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof PersonBirthDate other && Objects.equals(value, other.value));
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value != null ? value.toString() : StringUtils.EMPTY;
    }
}

