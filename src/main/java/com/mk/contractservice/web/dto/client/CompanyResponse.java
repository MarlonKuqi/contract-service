package com.mk.contractservice.web.dto.client;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Company client details (used for both creation response and read operations)")
public record CompanyResponse(
        @Schema(description = "Unique client identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Company name", example = "Acme Corporation")
        String name,

        @Schema(description = "Company email address", example = "contact@acme.com")
        String email,

        @Schema(description = "Company phone number", example = "+41221234567")
        String phone,

        @Schema(description = "Swiss company identifier (e.g., CHE number)", example = "CHE-123.456.789")
        String companyIdentifier
) implements ClientResponse {
}

