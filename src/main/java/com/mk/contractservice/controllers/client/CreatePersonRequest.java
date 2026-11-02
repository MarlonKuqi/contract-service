package com.mk.contractservice.controllers.client;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@JsonTypeName("PERSON")
@Schema(
        description = "Create person client request",
        example = """
                {
                  "type": "PERSON",
                  "name": "Alice Martin",
                  "email": "alice.martin@example.com",
                  "phone": "+41 21 123 45 67",
                  "birthDate": "1990-05-15"
                }
                """
)
public record CreatePersonRequest(
        @NotBlank(message = "Name is required")
        @Size(max = ClientName.MAX_LENGTH, message = "Name must not exceed " + ClientName.MAX_LENGTH + " characters")
        @Schema(description = "Person name", example = "Alice Martin", maxLength = ClientName.MAX_LENGTH,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotBlank(message = "Email is required")
        @Size(max = ClientEmail.MAX_LENGTH, message = "Email must not exceed " + ClientEmail.MAX_LENGTH + " characters")
        @Schema(description = "Person email address", example = "alice.martin@example.com", format = "email",
                maxLength = ClientEmail.MAX_LENGTH, requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotBlank(message = "Phone number is required")
        @Schema(
                description = "Person phone number in international format (+41, +33, +39 or +49). Normalized to E.164 in the response.",
                example = "+41 21 123 45 67",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String phone,

        @NotNull(message = "Birth date is required")
        @Schema(description = "Person birth date (must be in the past or present)", example = "1990-05-15",
                format = "date", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate birthDate
) implements CreateClientRequest {
}

