package com.mk.contractservice.acceptance.helper;

import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.SplittableRandom;
import java.util.UUID;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class TestDataHelper {

    public static String uniqueEmail(String prefix) {
        return prefix + "." + shortUuid() + "@example.com";
    }

    public static String uniqueCompanyIdentifier(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("%s-%s.%s.%s",
                prefix,
                uuid.substring(0, 3),
                uuid.substring(3, 6),
                uuid.substring(6, 9)).toUpperCase(Locale.ROOT);
    }

    public static String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static final SplittableRandom RANDOM = new SplittableRandom();

    public static String randomSwissPhoneNumber() {
        final int operator = 76 + RANDOM.nextInt(4); // 76, 77, 78, 79
        final int subscriber = RANDOM.nextInt(10_000_000); // 7 chiffres
        return "+41%d%07d".formatted(operator, subscriber);
    }
}

