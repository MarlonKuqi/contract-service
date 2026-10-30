package com.mk.contractservice.domain.client;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.mk.contractservice.domain.client.exception.InvalidClientPhoneNumberException;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientPhoneNumber {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    private static final EnumSet<SupportedFormat> SUPPORTED_REGIONS = EnumSet.allOf(SupportedFormat.class);

    String value;

    public static final int MAX_LENGTH = 16;

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
        if (isNotSupportedE164(value)) {
            throw new InvalidDomainObjectError("Corrupted phone number in persistence");
        }
        return new ClientPhoneNumber(value);
    }

    private static String normalize(@Nullable final String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidClientPhoneNumberException("Phone number must not be blank");
        }
        try {
            final Phonenumber.PhoneNumber parsed = PHONE_UTIL.parse(raw.strip(), null);
            return PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (final NumberParseException _) {
            throw new InvalidClientPhoneNumberException("Invalid phone number");
        }
    }

    private static void validate(final String e164) {
        if (isNotSupportedE164(e164)) {
            throw new InvalidClientPhoneNumberException("Invalid or unsupported phone number");
        }
    }

    private static boolean isNotSupportedE164(final String e164) {
        try {
            final Phonenumber.PhoneNumber parsed = PHONE_UTIL.parse(e164, null);
            return !PHONE_UTIL.isValidNumber(parsed)
                    || !SupportedFormat.supports(PHONE_UTIL.getRegionCodeForNumber(parsed))
                    || !PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164).equals(e164);
        } catch (final NumberParseException _) {
            return true;
        }
    }

    private enum SupportedFormat {
        CH, FR, IT, DE;

        private static final Map<String, SupportedFormat> BY_CODE = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity()));

        static boolean supports(@Nullable final String regionCode) {
            return regionCode != null && BY_CODE.containsKey(regionCode);
        }
    }
}
