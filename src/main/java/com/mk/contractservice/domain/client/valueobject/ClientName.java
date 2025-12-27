package com.mk.contractservice.domain.client.valueobject;

import com.mk.contractservice.domain.client.exception.InvalidClientNameException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientName {

    String value;

    private static final int MAX_LENGTH = 200;

    public static final Predicate<String> IS_NOT_VALID_LENGTH =
            name -> name.isEmpty() || name.length() > MAX_LENGTH;

    public static ClientName of(@Nullable final String rawValue) {
        return ValueObjectUtils.validateAndCreate(
                rawValue,
                ClientName::normalize,
                ClientName::validate,
                ClientName::new
        );
    }

    public static ClientName reconstituteFromDatabase(final String trustedValue) {
        return ValueObjectUtils.guardNotNull(trustedValue, ClientName::new, ClientName.class);
    }

    private static String normalize(@Nullable final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidClientNameException("Client name must not be null or blank");
        }
        return rawValue.trim();
    }

    private static void validate(final String normalized) {
        if (IS_NOT_VALID_LENGTH.test(normalized)) {
            throw new InvalidClientNameException("Client name too long (max " + MAX_LENGTH + " characters)");
        }
    }
}
