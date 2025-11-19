package com.mk.contractservice.web.dto.client;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonTypeName("COMPANY")
@Schema(
        description = "Create company client request",
        example = """
                {
                  "type": "COMPANY",
                  "name": "Acme Corporation",
                  "email": "contact@acme.com",
                  "phone": "+41221234567",
                  "companyIdentifier": "CHE-123.456.789"
                }
                """
)
public record CreateCompanyRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        @Schema(description = "Company name", example = "Acme Corporation", maxLength = 200, requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotBlank(message = "Email is required")
        @jakarta.validation.constraints.Email(message = "Email must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        @Schema(description = "Company email address", example = "contact@acme.com", format = "email", maxLength = 254, requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "\\+?[0-9 .()/-]{7,20}", message = "Phone number must be valid (7-20 characters, allowed: digits, +, -, ., (), /, spaces)")
        @Schema(description = "Company phone number", example = "+41221234567", pattern = "\\+?[0-9 .()/-]{7,20}", minLength = 7, maxLength = 20, requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,

        @NotBlank(message = "Company identifier is required")
        @Size(max = 64, message = "Company identifier must not exceed 64 characters")
        @Schema(description = "Swiss company identifier (e.g., CHE number)", example = "CHE-123.456.789", maxLength = 64, requiredMode = Schema.RequiredMode.REQUIRED)
        String companyIdentifier
) implements CreateClientRequest {
}
