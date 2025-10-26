package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Value Object representing a client's name (Person or Company).
 * Immutable and self-validating.
 *
 * <p>Use {@link #of(String)} factory method to create instances.
 * Direct construction is prevented to ensure validation.</p>
 */
@Embeddable
public final class ClientName {

    @Column(name = "name", nullable = false, length = 200)
    private final String value;

    /**
     * JPA no-args constructor.
     * Protected to prevent direct instantiation outside JPA context.
     */
    protected ClientName() {
        this.value = null;
    }

    /**
     * Private constructor to force use of {@link #of(String)} factory method.
     *
     * @param value the validated and normalized name value
     */
    private ClientName(final String value) {
        this.value = value;
    }

    /**
     * Factory method to create a validated ClientName.
     *
     * <p>Performs validation and normalization:
     * <ul>
     *   <li>Null/blank check</li>
     *   <li>Trim whitespace</li>
     *   <li>Length validation (max 200 chars)</li>
     * </ul>
     *
     * @param rawValue the raw name string
     * @return a validated ClientName instance
     * @throws IllegalArgumentException if validation fails
     */
    public static ClientName of(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Client name must not be null or blank");
        }

        final String normalized = rawValue.trim();

        if (normalized.length() > 200) {
            throw new IllegalArgumentException("Client name too long (max 200 characters)");
        }

        return new ClientName(normalized);
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

