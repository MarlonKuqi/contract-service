package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.exception.InvalidIdempotencyKeyException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class IdempotencyKey {

    public static final int MAX_LENGTH = 255;

    String value;

    public static IdempotencyKey of(@Nullable final String rawValue) {
        return ValueObjectUtils.validateAndCreate(
                rawValue,
                IdempotencyKey::normalize,
                IdempotencyKey::validate,
                IdempotencyKey::new
        );
    }

    public static IdempotencyKey reconstituteFromDatabase(@Nullable final String trustedValue) {
        return new IdempotencyKey(notNull(trustedValue));
    }

    private static String normalize(@Nullable final String rawValue) {
        if (rawValue == null) {
            throw new InvalidIdempotencyKeyException("The idempotency key must not be null");
        }
        return rawValue;
    }

    private static void validate(final String value) {
        if (value.isBlank() || value.length() > MAX_LENGTH) {
            throw new InvalidIdempotencyKeyException(
                    "The idempotency key must not be blank nor exceed " + MAX_LENGTH + " characters");
        }
    }
}
