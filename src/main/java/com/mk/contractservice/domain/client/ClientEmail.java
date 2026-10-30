package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.client.exception.InvalidClientEmailException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientEmail {

    String value;

    public static final int MAX_LENGTH = 254;
    private static final int LOCAL_PART_MAX_LENGTH = 64;

    private static final Pattern LOCAL_PART =
            Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~-]++(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]++)*+");
    private static final Pattern DOMAIN =
            Pattern.compile("(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)++(?:[a-z]{2,63}|xn--[a-z0-9-]{1,59})");

    public static ClientEmail of(@Nullable final String rawValue) {
        return ValueObjectUtils.validateAndCreate(
                rawValue,
                ClientEmail::normalize,
                ClientEmail::validate,
                ClientEmail::new
        );
    }

    public static ClientEmail reconstituteFromDatabase(final String trustedValue) {
        return new ClientEmail(notNull(trustedValue));
    }

    private static String normalize(@Nullable final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw InvalidClientEmailException.forBlank();
        }
        return rawValue.strip().toLowerCase(Locale.ROOT);
    }

    private static void validate(final String email) {
        if (email.length() > MAX_LENGTH) {
            throw InvalidClientEmailException.forTooLong(MAX_LENGTH);
        }
        final int at = email.lastIndexOf('@');
        if (at < 0 || isInvalidLocalPart(email.substring(0, at)) || isInvalidDomain(email.substring(at + 1))) {
            throw InvalidClientEmailException.forInvalidFormat();
        }
    }

    private static boolean isInvalidLocalPart(final String localPart) {
        return localPart.length() > LOCAL_PART_MAX_LENGTH || !LOCAL_PART.matcher(localPart).matches();
    }

    private static boolean isInvalidDomain(final String domain) {
        return !DOMAIN.matcher(domain).matches();
    }
}
