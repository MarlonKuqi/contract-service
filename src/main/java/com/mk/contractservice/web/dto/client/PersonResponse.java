package com.mk.contractservice.web.dto.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Person client details")
public record PersonResponse(
        @Schema(description = "Unique client identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Person name", example = "Alice Martin")
        ClientName name,

        @Schema(description = "Person email address", example = "alice.martin@example.com")
        Email email,

        @Schema(description = "Person phone number", example = "+41791234567")
        PhoneNumber phone,

        @Schema(description = "Person birth date", example = "1990-05-15")
        PersonBirthDate birthDate
) implements ClientResponse {
}

