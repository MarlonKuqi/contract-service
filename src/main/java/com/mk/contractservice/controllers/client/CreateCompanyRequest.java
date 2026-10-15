package com.mk.contractservice.controllers.client;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
        @Size(max = 200, message = "Name must not exceed 200 characters")
        @Schema(description = "Company name", example = "Acme Corporation", maxLength = 200,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        @Schema(description = "Company email address", example = "contact@acme.com", format = "email", maxLength = 254,
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
        @Size(max = 64, message = "Company identifier must not exceed 64 characters")
        @Schema(description = "Swiss company identifier (e.g., CHE number)", example = "CHE-123.456.789",
                maxLength = 64, requiredMode = Schema.RequiredMode.REQUIRED)
        String companyIdentifier
) implements CreateClientRequest {
}

