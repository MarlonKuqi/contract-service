package com.mk.contractservice.controllers.client;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.CompanyIdentifier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonTypeName("COMPANY")
@Schema(
        description = "Create company client request",
        example = """
                {
                  "type": "COMPANY",
                  "name": "Acme Corporation",
                  "email": "contact@acme.com",
                  "phone": "+41 22 123 45 67",
                  "companyIdentifier": "CHE-123.456.789"
                }
                """
)
public record CreateCompanyRequest(
        @NotBlank(message = "Name is required")
        @Size(max = ClientName.MAX_LENGTH, message = "Name must not exceed " + ClientName.MAX_LENGTH + " characters")
        @Schema(description = "Company name", example = "Acme Corporation", maxLength = ClientName.MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotBlank(message = "Email is required")
        @Size(max = ClientEmail.MAX_LENGTH, message = "Email must not exceed " + ClientEmail.MAX_LENGTH + " characters")
        @Schema(description = "Company email address", example = "contact@acme.com", format = "email",
                maxLength = ClientEmail.MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotBlank(message = "Phone number is required")
        @Schema(
                description = "Company phone number in international format (+41, +33, +39 or +49). Normalized to E.164 in the response.",
                example = "+41 22 123 45 67",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String phone,

        @NotBlank(message = "Company identifier is required")
        @Size(max = CompanyIdentifier.MAX_LENGTH, message = "Company identifier must not exceed 64 characters")
        @Schema(description = "Swiss company identifier (e.g., CHE number). Case-insensitive: stored and returned in upper case, "
                + "and an identifier differing only by case from an existing one is rejected with 409 companyIdentifierAlreadyExists",
                example = "CHE-123.456.789",
                maxLength = 64, requiredMode = Schema.RequiredMode.REQUIRED)
        String companyIdentifier
) implements CreateClientRequest {
}

