package com.mk.contractservice.domain.client;

import java.time.LocalDate;
import java.util.UUID;

public record PersonDetails(
        UUID id,
        String name,
        String email,
        String phone,
        LocalDate birthDate,
        long version
) implements ClientDetails {
}
