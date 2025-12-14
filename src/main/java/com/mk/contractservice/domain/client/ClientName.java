package com.mk.contractservice.domain.client;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mk.contractservice.domain.exception.InvalidClientNameException;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

public final class ClientName {

    private static final int MAX_LENGTH = 200;

    public static final Predicate<String> IS_NOT_VALID_LENGTH =
            name -> name.isEmpty() || name.length() > MAX_LENGTH;

    @JsonValue
    private final String value;

    private ClientName(final String value) {
        this.value = value;
    }

    public static ClientName of(@Nullable final String rawValue) {
        final String normalized = normalize(rawValue);
        validate(normalized);
        return new ClientName(normalized);
    }

    private static String normalize(@Nullable final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidClientNameException("Client name must not be null or blank");
        }
        return rawValue.trim();
    }

    private static void validate(final String normalized) {
        if (IS_NOT_VALID_LENGTH.test(normalized)) {
            throw new InvalidClientNameException("Client name too long (max " + MAX_LENGTH + " characters)");
        }
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        return this == o || (o instanceof ClientName other && Objects.equals(value, other.value));
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
