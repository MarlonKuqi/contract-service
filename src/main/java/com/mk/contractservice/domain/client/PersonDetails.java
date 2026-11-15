package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.shared.Residence;

import java.time.LocalDate;
import java.util.UUID;

public record PersonDetails(
        UUID id,
        String name,
        String email,
        String phone,
        LocalDate birthDate,
        Residence residence,
        long version
) implements ClientDetails {
}
