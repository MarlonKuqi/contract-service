package com.mk.contractservice.web.dto.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for reading a Person client (GET /v1/clients/{id}).
 * Contains all fields including the ID.
 */
public record PersonResponse(
        UUID id,
        ClientName name,
        Email email,
        PhoneNumber phone,
        LocalDate birthDate
) implements ClientResponse {
}

