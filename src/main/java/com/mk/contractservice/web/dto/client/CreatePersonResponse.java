package com.mk.contractservice.web.dto.client;

import java.time.LocalDate;

public record CreatePersonResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDate birthDate
) implements CreateClientResponse {
}