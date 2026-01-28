package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.client.exception.InvalidClientPhoneNumberException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientPhoneNumber {

    String prefix;
    String base;
    String value;

    private static final String SWISS_PREFIX = "+41";
    private static final String FRENCH_PREFIX = "+33";
    private static final String ITALIAN_PREFIX = "+39";
    private static final String GERMAN_PREFIX = "+49";

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 14;

    private static final Predicate<String> IS_SWISS_PHONE =
            phone -> phone.length() == 12 && phone.matches(SWISS_PREFIX.replace("+", "\\+") + "\\d{9}");

    private static final Predicate<String> IS_FRENCH_PHONE =
            phone -> phone.length() == 12 && phone.matches(FRENCH_PREFIX.replace("+", "\\+") + "\\d{9}");

    private static final Predicate<String> IS_ITALIAN_PHONE =
            phone -> (phone.length() == 12 || phone.length() == 13) && phone.matches(ITALIAN_PREFIX.replace("+", "\\+") + "\\d{9,10}");

    private static final Predicate<String> IS_GERMAN_PHONE =
            phone -> (phone.length() == 13 || phone.length() == 14) && phone.matches(GERMAN_PREFIX.replace("+", "\\+") + "\\d{10,11}");

    private static final Predicate<String> IS_INVALID_PHONE = rawValue ->
            Stream.of(IS_SWISS_PHONE, IS_FRENCH_PHONE, IS_ITALIAN_PHONE, IS_GERMAN_PHONE)
                    .noneMatch(p -> p.test(rawValue));

    public static ClientPhoneNumber of(@Nullable final String rawValue) {
        return ValueObjectUtils.validateAndCreate(
                rawValue,
                ClientPhoneNumber::normalize,
                ClientPhoneNumber::validate, ClientPhoneNumber::create
        );
    }

    public static ClientPhoneNumber reconstituteFromDatabase(final String trustedValue) {
        return create(notNull(trustedValue));
    }

    private static ClientPhoneNumber create(final String normalizedValue) {
        final String prefix = normalizedValue.substring(0, 3);
        String base = normalizedValue.substring(3);
        return new ClientPhoneNumber(prefix, base, normalizedValue);
    }

    private static String normalize(@Nullable final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidClientPhoneNumberException("Phone number must not be null or blank");
        }
        return rawValue.trim().replace(" ", "");
    }

    private static void validate(final String normalized) {
        if (IS_INVALID_PHONE.test(normalized)) {
            throw new InvalidClientPhoneNumberException(
                    "Invalid phone number. Must be Swiss (+41 + 9 digits), French (+33 + 9 digits), " +
                            "Italian (+39 + 9-10 digits) or German (+49 + 10-11 digits). Got: " + normalized
            );
        }
    }

    @Override
    public String toString() {
        return switch (prefix) {
            case SWISS_PREFIX -> prefix +
                    base.substring(0, 2) + " " +
                    base.substring(2, 5) + " " +
                    base.substring(5, 7) + " " +
                    base.substring(7);
            case FRENCH_PREFIX -> prefix +
                    base.charAt(0) + " " +
                    base.substring(1, 3) + " " +
                    base.substring(3, 5) + " " +
                    base.substring(5, 7) + " " +
                    base.substring(7);
            case ITALIAN_PREFIX -> prefix +
                    (base.length() == 10
                            ? base.substring(0, 3) + " " + base.substring(3, 6) + " " + base.substring(6)
                            : base.substring(0, 2) + " " + base.substring(2, 6) + " " + base.substring(6));
            case GERMAN_PREFIX -> prefix + " " +
                    base.substring(0, Math.min(3, base.length())) + " " +
                    base.substring(Math.min(3, base.length()));
            default -> value;
        };
    }
}
