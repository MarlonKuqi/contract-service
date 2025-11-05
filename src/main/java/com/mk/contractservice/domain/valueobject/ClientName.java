package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mk.contractservice.domain.exception.InvalidClientNameException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class ClientName {

    @Column(name = "name", nullable = false, length = 200)
    @JsonValue
    private final String value;

    private ClientName(final String value) {
        this.value = value;
    }

    public static ClientName of(final String rawValue) {
        final String normalized = normalize(rawValue);
        validate(normalized);
        return new ClientName(normalized);
    }

    private static String normalize(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidClientNameException("Client name must not be null or blank");
        }
        return rawValue.trim();
    }

    private static void validate(final String normalized) {
        if (normalized.length() > 200) {
            throw new InvalidClientNameException("Client name too long (max 200 characters)");
        }
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof ClientName other && Objects.equals(value, other.value));
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
