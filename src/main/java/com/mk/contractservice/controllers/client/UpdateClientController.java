package com.mk.contractservice.controllers.client;

import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.client.shared.ClientSwaggerTags;
import com.mk.contractservice.controllers.shared.EntityTags;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.features.client.UpdateClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = ClientSwaggerTags.NAME, description = ClientSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ClientEndpoints.CLIENTS_BASE)
@RequiredArgsConstructor
public class UpdateClientController {

    private final UpdateClient updateClient;

    @Operation(
            summary = "Update a client (all fields except birthDate/companyIdentifier)",
            description = "Updates the common fields of a client (name, email, phone). "
                    + "birthDate and companyIdentifier cannot be updated as per business rules. "
                    + "If all values are identical to the current ones after normalization (email case, surrounding spaces, phone format), the client is left unchanged. "
                    + "Works for both Person and Company clients. "
                    + "Requires If-Match with the ETag of the client as read. The response carries no ETag: read the client again before another modification."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Client updated successfully (no content returned)"
    )
    @ApiResponse(
            responseCode = "409",
            description = "The client is held by a contract import batch for longer than the lock timeout (code resourceBusy). Retry in a few seconds",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "412",
            description = "The client was modified since the ETag sent in If-Match was read (code preconditionFailed)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "428",
            description = "If-Match is missing or is the wildcard * (code preconditionRequired)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
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
            @Parameter(in = ParameterIn.HEADER, required = true,
                    description = "ETag of the client as read (GET). Wildcard * is not accepted.")
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) @Nullable final String ifMatch,
            @Valid @RequestBody final Request request
    ) {
        updateClient.execute(new UpdateClient.Command(
                id,
                EntityTags.requiredVersion(ifMatch),
                request.name(),
                request.email(),
                request.phone()
        ));
        return ResponseEntity.noContent().build();
    }

    public record Request(
            @NotBlank(message = "Name is required")
            @Size(max = ClientName.MAX_LENGTH, message = "Name must not exceed 200 characters")
            @Schema(description = "Client name", example = "John Doe", maxLength = 200,
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @NotBlank(message = "Email is required")
            @Size(max = ClientEmail.MAX_LENGTH, message = "Email must not exceed 254 characters")
            @Schema(description = "Client email address", example = "john.doe@example.com",
                    format = "email", maxLength = 254, requiredMode = Schema.RequiredMode.REQUIRED)
            String email,

            @NotBlank(message = "Phone number is required")
            @Schema(description = "Client phone number in international format (+41, +33, +39 or +49). Normalized to E.164 in the response.",
                    example = "+41791234567", requiredMode = Schema.RequiredMode.REQUIRED)
            String phone
    ) {
    }

}
