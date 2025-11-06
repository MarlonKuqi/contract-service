package com.mk.contractservice.web.dto.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Created company client details (returned after successful creation)")
public record CreateCompanyResponse(
        @Schema(description = "Unique identifier of the created company client", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Company name", example = "Acme Corporation")
        ClientName name,

        @Schema(description = "Company email address", example = "contact@acme.com")
        Email email,

        @Schema(description = "Company phone number", example = "+41221234567")
        PhoneNumber phone,

        @Schema(description = "Swiss company identifier (e.g., CHE number)", example = "CHE-123.456.789")
        CompanyIdentifier companyIdentifier
) implements CreateClientResponse {
}