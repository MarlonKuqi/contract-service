package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Value Object representing a phone number.
 * Immutable and self-validating.
 *
 * <p>Use {@link #of(String)} factory method to create instances.
 * Direct construction is prevented to ensure validation.</p>
 */
@Embeddable
public final class PhoneNumber {

    @Column(name = "phone", nullable = false, length = 32)
    private final String value;

    /**
     * JPA no-args constructor.
     * Protected to prevent direct instantiation outside JPA context.
     */
    protected PhoneNumber() {
        this.value = null;
    }

    /**
     * Private constructor to force use of {@link #of(String)} factory method.
     *
     * @param value the validated and normalized phone number value
     */
    private PhoneNumber(final String value) {
        this.value = value;
    }

    /**
     * Factory method to create a validated PhoneNumber.
     *
     * <p>Performs validation and normalization:
     * <ul>
     *   <li>Null/blank check</li>
     *   <li>Trim whitespace</li>
     *   <li>Format validation (international format accepted)</li>
     *   <li>Length validation (7-20 chars)</li>
     * </ul>
     *
     * @param rawValue the raw phone number string
     * @return a validated PhoneNumber instance
     * @throws IllegalArgumentException if validation fails
     */
    public static PhoneNumber of(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Phone number must not be null or blank");
        }

        final String normalized = rawValue.trim();

        // Pattern accepts: +, digits, spaces, dots, parentheses, hyphens, slashes
        if (!normalized.matches("\\+?[0-9 .()/-]{7,20}")) {
            throw new IllegalArgumentException("Invalid phone number format: " + rawValue);
        }

        return new PhoneNumber(normalized);
    }

    @JsonValue
    public String value() {
        return value;
    }
    
    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof PhoneNumber other && Objects.equals(value, other.value));
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

