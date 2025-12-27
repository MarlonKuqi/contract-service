package com.mk.contractservice.domain.client.valueobject;

import com.mk.contractservice.domain.client.exception.InvalidEmailException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Predicate;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientEmail {

    String value;

    private static final int MAX_LENGTH = 254;
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    public static final Predicate<String> IS_INVALID_LENGTH =
            email -> email.isEmpty() || email.length() > MAX_LENGTH;


    public static final Predicate<String> HAS_INVALID_FORMAT =
            email -> !email.matches(EMAIL_PATTERN);

    public static ClientEmail of(@Nullable final String rawValue) {
        return ValueObjectUtils.validateAndCreate(
                rawValue,
                ClientEmail::normalize,
                ClientEmail::validate,
                ClientEmail::new
        );
    }

    public static ClientEmail reconstituteFromDatabase(final String trustedValue) {
        return ValueObjectUtils.guardNotNull(trustedValue, ClientEmail::new, ClientEmail.class);
    }

    private static String normalize(@Nullable final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw InvalidEmailException.forBlank();
        }
        return rawValue.trim().toLowerCase(Locale.ROOT);
    }

    private static void validate(final String normalizedValue) {
        if (IS_INVALID_LENGTH.test(normalizedValue)) {
            throw new InvalidEmailException("Email too long (max " + MAX_LENGTH + " characters per RFC 5321)");
        }
        if (HAS_INVALID_FORMAT.test(normalizedValue)) {
            throw InvalidEmailException.forInvalidFormat(normalizedValue);
        }
    }
}
