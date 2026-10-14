package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.client.exception.InvalidClientPhoneNumberException;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientPhoneNumber {

    String value;

    public static ClientPhoneNumber of(@Nullable final String raw) {
        return ValueObjectUtils.validateAndCreate(
                raw,
                ClientPhoneNumber::normalize,
                ClientPhoneNumber::validate,
                ClientPhoneNumber::new
        );
    }

    public static ClientPhoneNumber reconstituteFromDatabase(final String trusted) {
        final String value = notNull(trusted);
        if (isInvalid(value)) {
            throw new InvalidDomainObjectError("Corrupted phone number in persistence");
        }
        return new ClientPhoneNumber(value);
    }

    private static String normalize(@Nullable final String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidClientPhoneNumberException("Phone number must not be blank");
        }
        return raw.strip().replaceAll("[\\s.\\-()]", "");
    }

    private static void validate(final String normalized) {
        if (isInvalid(normalized)) {
            throw new InvalidClientPhoneNumberException("Invalid or unsupported phone number");
        }
    }

    private static boolean isInvalid(final String e164) {
        return Arrays.stream(CountryDialPlan.values()).noneMatch(plan -> plan.accepts(e164));
    }

    private enum CountryDialPlan {

        SWITZERLAND("\\+41\\d{9}"),
        FRANCE("\\+33[1-9]\\d{8}"),
        ITALY("\\+39\\d{9,10}"),
        GERMANY("\\+49\\d{10,11}");

        private final Pattern pattern;

        CountryDialPlan(final String regex) {
            this.pattern = Pattern.compile(regex);
        }

        boolean accepts(final String e164) {
            return pattern.matcher(e164).matches();
        }
    }
}
