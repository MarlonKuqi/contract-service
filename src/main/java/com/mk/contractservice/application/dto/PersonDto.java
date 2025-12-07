package com.mk.contractservice.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PersonDto(
        UUID id,
        String name,
        String email,
        String phone,
        LocalDate birthDate
) implements ClientDto {
}


