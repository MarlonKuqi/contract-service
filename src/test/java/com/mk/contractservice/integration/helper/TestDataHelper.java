package com.mk.contractservice.integration.helper;

import java.util.UUID;

public final class TestDataHelper {

    private TestDataHelper() {
        // Utility class
    }

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
}

