package com.mk.contractservice.web.dto.client;

public record CreateCompanyResponse(
        Long id,
        String name,
        String email,
        String phone,
        String companyIdentifier
) implements CreateClientResponse {
}