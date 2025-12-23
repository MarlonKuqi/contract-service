package com.mk.contractservice.domain.client.valueobject;

import com.mk.contractservice.domain.client.exception.InvalidEmailException;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

public final class ClientEmail {

    private static final int MAX_LENGTH = 254;
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    public static final Predicate<String> IS_INVALID_LENGTH =
            email -> email.isEmpty() || email.length() > MAX_LENGTH;


    public static final Predicate<String> HAS_INVALID_FORMAT =
            email -> !email.matches(EMAIL_PATTERN);


    private final String value;

    private ClientEmail(final String value) {
        this.value = value;
    }

    public static ClientEmail of(@Nullable final String rawValue) {
        final String normalizedValue = normalize(rawValue);
        validate(normalizedValue, rawValue);
        return new ClientEmail(normalizedValue);
    }

    private static String normalize(@Nullable final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
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

    public String value() {
        return value;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        return this == o || (o instanceof ClientEmail other && Objects.equals(value, other.value));
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
