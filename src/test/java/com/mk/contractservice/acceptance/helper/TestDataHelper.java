package com.mk.contractservice.acceptance.helper;

import lombok.NoArgsConstructor;

import java.util.SplittableRandom;
import java.util.UUID;
import java.util.stream.Collectors;

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
                uuid.substring(6, 9));
    }

    public static String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }


    public static String randomSwissPhoneNumber() {
        return "+41" + new SplittableRandom()
                .ints(9, 0, 10)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());
    }

    public static String randomFrenchPhoneNumber() {
        return "+33" + new SplittableRandom()
                .ints(9, 0, 10)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());
    }
}

