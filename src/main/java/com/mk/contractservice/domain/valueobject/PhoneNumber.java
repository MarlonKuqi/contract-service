package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mk.contractservice.domain.exception.InvalidPhoneNumberException;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Predicate;

public final class PhoneNumber {

    private static final String PHONE_PATTERN = "\\+?[0-9 .()/-]{7,20}";

    public static final Predicate<String> IS_BLANK =
        phone -> phone == null || phone.isBlank();

    public static final Predicate<String> HAS_INVALID_FORMAT =
        phone -> phone == null || !phone.matches(PHONE_PATTERN);

    private final String value;

    private PhoneNumber(final String value) {
        this.value = value;
    }

    public static PhoneNumber of(final String rawValue) {
        final String normalized = normalize(rawValue);
        validate(normalized, rawValue);
        return new PhoneNumber(normalized);
    }

    private static String normalize(final String rawValue) {
        if (IS_BLANK.test(rawValue)) {
            throw new InvalidPhoneNumberException("Phone number must not be null or blank");
        }
        return rawValue.trim();
    }

    private static void validate(final String normalized, final String rawValue) {
        if (HAS_INVALID_FORMAT.test(normalized)) {
            throw new InvalidPhoneNumberException("Invalid phone number format: " + rawValue);
        }
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