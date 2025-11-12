package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mk.contractservice.domain.exception.InvalidCompanyIdentifierException;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Predicate;

public final class CompanyIdentifier {

    private static final int MAX_LENGTH = 64;

    public static final Predicate<String> IS_BLANK =
        identifier -> identifier == null || identifier.isBlank();

    public static final Predicate<String> IS_NOT_VALID =
        identifier -> identifier == null || identifier.isEmpty() || identifier.length() > MAX_LENGTH;

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
        if (IS_BLANK.test(rawValue)) {
            throw new InvalidCompanyIdentifierException("Company identifier must not be null or blank");
        }
        return rawValue.trim();
    }

    private static void validate(final String normalized) {
        if (IS_NOT_VALID.test(normalized)) {
            throw new InvalidCompanyIdentifierException("Company identifier too long (max " + MAX_LENGTH + " characters)");
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
