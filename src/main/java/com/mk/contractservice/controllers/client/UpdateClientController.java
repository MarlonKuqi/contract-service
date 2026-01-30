package com.mk.contractservice.controllers.client;

import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.client.shared.ClientSwaggerTags;
import com.mk.contractservice.features.client.UpdateClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = ClientSwaggerTags.NAME, description = ClientSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ClientEndpoints.CLIENTS_BASE)
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class UpdateClientController {

    UpdateClient updateClient;

    @Operation(
            summary = "Update a client (all fields except birthDate/companyIdentifier)",
            description = "Updates the common fields of a client (name, email, phone). "
                    + "birthDate and companyIdentifier cannot be updated as per business rules. "
                    + "Works for both Person and Company clients."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Client updated successfully (no content returned)"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input data (validation failed)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Client not found",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "422",
            description = "Business validation failed (e.g., invalid email or phone format)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PutMapping(ClientEndpoints.PATH_VAR_ID)
    public ResponseEntity<Void> updateClient(
            @PathVariable final UUID id,
            @Valid @RequestBody final Request request
    ) {
        updateClient.execute(new UpdateClient.Command(
                id,
                request.name(),
                request.email(),
                request.phone()
        ));
        return ResponseEntity.noContent().build();
    }

    public record Request(
            @NotBlank(message = "Name is required")
            @Size(max = 200, message = "Name must not exceed 200 characters")
            @Schema(description = "Client name", example = "John Doe", maxLength = 200, requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be a valid email address")
            @Size(max = 254, message = "Email must not exceed 254 characters")
            @Schema(description = "Client email address", example = "john.doe@example.com", format = "email", maxLength = 254, requiredMode = Schema.RequiredMode.REQUIRED)
            String email,

            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "\\+?[0-9 .()/-]{7,20}", message = "Phone number must be valid (7-20 characters, allowed: digits, +, -, ., (), /, spaces)")
            @Schema(description = "Client phone number", example = "+41791234567", pattern = "\\+?[0-9 .()/-]{7,20}", minLength = 7, maxLength = 20, requiredMode = Schema.RequiredMode.REQUIRED)
            String phone
    ) {
    }

}
