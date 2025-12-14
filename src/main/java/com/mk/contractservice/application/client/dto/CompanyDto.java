package com.mk.contractservice.application.client.dto;

import java.util.UUID;

public record CompanyDto(
        UUID id,
        String name,
        String email,
        String phone,
        String companyIdentifier
) implements ClientDto {
}

