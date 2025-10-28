package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class CompanyIdentifier {

    @Column(name = "company_identifier", nullable = false, updatable = false, unique = true, length = 64)
    private final String value;

    private CompanyIdentifier(final String value) {
        this.value = value;
    }

    public static CompanyIdentifier of(final String rawValue) {
        final String normalized = normalize(rawValue);
        validate(normalized);
        return new CompanyIdentifier(normalized);
    }

    private static String normalize(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Company identifier must not be null or blank");
        }
        return rawValue.trim();
    }

    private static void validate(final String normalized) {
        if (normalized.length() > 64) {
            throw new IllegalArgumentException("Company identifier too long (max 64 characters)");
        }
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof CompanyIdentifier other && Objects.equals(value, other.value));
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
