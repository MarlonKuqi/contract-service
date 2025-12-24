package com.mk.contractservice.domain.client.valueobject;

import com.mk.contractservice.domain.client.exception.InvalidCompanyIdentifierException;
import com.mk.contractservice.domain.shared.ValueObject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CompanyIdentifier {

    String value;

    private static final int MAX_LENGTH = 64;

    public static final Predicate<String> IS_NOT_VALID =
            identifier -> identifier.isEmpty() || identifier.length() > MAX_LENGTH;

    public static CompanyIdentifier of(@Nullable final String rawValue) {
        return ValueObject.validateAndCreate(
                rawValue,
                CompanyIdentifier::normalize,
                CompanyIdentifier::validate,
                CompanyIdentifier::new
        );
    }

    public static CompanyIdentifier reconstituteFromDatabase(final String trustedValue) {
        return ValueObject.guardNotNull(trustedValue, CompanyIdentifier::new, CompanyIdentifier.class);
    }

    private static String normalize(@Nullable final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidCompanyIdentifierException("Company identifier must not be null or blank");
        }
        return rawValue.trim();
    }

    private static void validate(final String normalized) {
        if (IS_NOT_VALID.test(normalized)) {
            throw new InvalidCompanyIdentifierException("Company identifier too long (max " + MAX_LENGTH + " characters)");
        }
    }
}
