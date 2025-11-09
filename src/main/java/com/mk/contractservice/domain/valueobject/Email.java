package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mk.contractservice.domain.exception.InvalidEmailException;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Objects;

public final class Email {

    private final String value;

    private Email(final String value) {
        this.value = value;
    }

    public static Email of(final String rawValue) {
        final String normalizedValue = normalize(rawValue);
        validate(normalizedValue, rawValue);
        return new Email(normalizedValue);
    }

    private static String normalize(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw InvalidEmailException.forBlank();
        }
        return rawValue.trim().toLowerCase(Locale.ROOT);
    }

    private static void validate(final String normalizedValue, final String rawValue) {
        if (normalizedValue.length() > 254) {
            throw new InvalidEmailException("Email too long (max 254 characters per RFC 5321)");
        }

        if (!normalizedValue.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw InvalidEmailException.forInvalidFormat(rawValue);
        }
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof Email other && Objects.equals(value, other.value));
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value != null ? value : StringUtils.EMPTY;
    }
}
