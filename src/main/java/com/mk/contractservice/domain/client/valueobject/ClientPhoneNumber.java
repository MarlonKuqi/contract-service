package com.mk.contractservice.domain.client.valueobject;

import com.mk.contractservice.domain.client.exception.InvalidClientPhoneNumberException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientPhoneNumber {

    String value;

    private static final String PHONE_PATTERN = "\\+?[0-9 .()/-]{7,20}";

    public static final Predicate<String> HAS_INVALID_FORMAT =
            phone -> !phone.matches(PHONE_PATTERN);

    public static ClientPhoneNumber of(@Nullable final String rawValue) {
        return ValueObjectUtils.validateAndCreate(
                rawValue,
                ClientPhoneNumber::normalize,
                ClientPhoneNumber::validate,
                ClientPhoneNumber::new
        );
    }

    public static ClientPhoneNumber reconstituteFromDatabase(final String trustedValue) {
        return ValueObjectUtils.guardNotNull(trustedValue, ClientPhoneNumber::new, ClientPhoneNumber.class);
    }

    private static String normalize(@Nullable final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidClientPhoneNumberException("Phone number must not be null or blank");
        }
        return rawValue.trim();
    }

    private static void validate(final String normalized) {
        if (HAS_INVALID_FORMAT.test(normalized)) {
            throw new InvalidClientPhoneNumberException("Invalid phone number format: " + normalized);
        }
    }
}
