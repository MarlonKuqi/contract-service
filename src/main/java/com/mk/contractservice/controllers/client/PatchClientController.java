package com.mk.contractservice.controllers.client;

import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.client.shared.ClientSwaggerTags;
import com.mk.contractservice.features.client.PatchClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@Tag(name = ClientSwaggerTags.NAME, description = ClientSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ClientEndpoints.CLIENTS_BASE)
@RequiredArgsConstructor
public class PatchClientController {

    private final PatchClient patchClient;

    @Operation(
            summary = "Partially update a client (PATCH)",
            description = "Updates only the provided fields of a client. "
                    + "All fields are optional. If no fields are provided, the operation is idempotent (no change). "
                    + "birthDate and companyIdentifier cannot be updated as per business rules. "
                    + "Works for both Person and Company clients."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Client patched successfully (no content returned, even if no fields were changed)"
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
    @PatchMapping(ClientEndpoints.PATH_VAR_ID)
    public ResponseEntity<Void> patchClient(
            @PathVariable("id") final UUID clientId,
            @Valid @RequestBody final Request request
    ) {
        patchClient.execute(new PatchClient.Command(
                clientId,
                request.name().orElse(null),
                request.email().orElse(null),
                request.phone().orElse(null)
        ));
        return ResponseEntity.noContent().build();
    }

    public record Request(
            @Schema(description = "Client name (optional)", example = "John Doe", maxLength = 200)
            Optional<String> name,

            @Schema(description = "Client email address (optional)", example = "john.doe@example.com",
                    format = "email", maxLength = 254)
            Optional<String> email,

            @Schema(description = "Client phone number (optional)", example = "+41791234567",
                    pattern = "\\+?[0-9 .()/-]{7,20}", minLength = 7, maxLength = 20)
            Optional<String> phone
    ) {
    }
}

