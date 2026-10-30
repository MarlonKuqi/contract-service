package com.mk.contractservice.controllers.client.shared;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Person client details (used for both creation response and read operations)")
public record PersonResponse(
        @Schema(description = "Unique client identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Person name", example = "Alice Martin")
        String name,

        @Schema(description = "Person email address", example = "alice.martin@example.com")
        String email,

        @Schema(description = "Person phone number", example = "+41791234567")
        String phone,

        @Schema(description = "Person birth date", example = "1990-05-15")
        LocalDate birthDate
) implements ClientResponse {
}

