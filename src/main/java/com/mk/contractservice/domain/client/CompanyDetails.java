package com.mk.contractservice.domain.client;

import java.util.UUID;

public record CompanyDetails(
        UUID id,
        String name,
        String email,
        String phone,
        String companyIdentifier,
        long version
) implements ClientDetails {
}
