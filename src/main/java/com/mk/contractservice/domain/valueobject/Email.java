package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mk.contractservice.domain.exception.InvalidEmailException;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

public final class Email {

    private static final int MAX_LENGTH = 254;
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    public static final Predicate<String> IS_BLANK =
        email -> email == null || email.isBlank();

    public static final Predicate<String> IS_INVALID_LENGTH =
        email -> email == null || email.isEmpty() || email.length() > MAX_LENGTH;


    public static final Predicate<String> HAS_INVALID_FORMAT =
        email -> email == null || !email.matches(EMAIL_PATTERN);


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
        if (IS_BLANK.test(rawValue)) {
            throw InvalidEmailException.forBlank();
        }
        return rawValue.trim().toLowerCase(Locale.ROOT);
    }

    private static void validate(final String normalizedValue, final String rawValue) {
        if (IS_INVALID_LENGTH.test(normalizedValue)) {
            throw new InvalidEmailException("Email too long (max " + MAX_LENGTH + " characters per RFC 5321)");
        }

        if (HAS_INVALID_FORMAT.test(normalizedValue)) {
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
