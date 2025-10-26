package com.mk.contractservice.web.dto.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import java.util.UUID;

/**
 * Response DTO for reading a Company client (GET /v1/clients/{id}).
 * Contains all fields including the ID.
 */
public record CompanyResponse(
        UUID id,
        ClientName name,
        Email email,
        PhoneNumber phone,
        String companyIdentifier
) implements ClientResponse {
}

