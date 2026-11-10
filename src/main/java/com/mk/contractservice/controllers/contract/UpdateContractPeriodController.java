package com.mk.contractservice.controllers.contract;

import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractSwaggerTags;
import com.mk.contractservice.controllers.shared.EntityTags;
import com.mk.contractservice.features.contract.UpdateContractPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = ContractSwaggerTags.NAME, description = ContractSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ContractEndpoints.CONTRACTS_BASE)
@RequiredArgsConstructor
public class UpdateContractPeriodController {

    private final UpdateContractPeriod updateContractPeriod;

    @Operation(
            summary = "Replace the period of a contract",
            description = "Replaces the start and end dates of a contract. startDate is required; a missing or null endDate makes the contract open-ended. " + "Business rules: only active contracts can be changed, the start date may be moved into the past, " + "the end date cannot be in the past and must be after the start date. " + "A change updates the internal last modification date (updatedSince filter); sending the current period is a no-op. " + "Requires If-Match with the ETag of the contract as read (GET). The response carries no ETag: read the contract again before another change."
    )
    @ApiResponse(responseCode = "204", description = "Period replaced successfully")
    @ApiResponse(
            responseCode = "404",
            description = "Contract not found (code contractNotFound)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(
                    implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Contract is expired and cannot be modified (code contractExpired)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(
                    implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "412",
            description = "The contract was modified since the ETag sent in If-Match was read (code preconditionFailed)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(
                    implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "422",
            description = "Malformed body or missing startDate (code validationError), or invalid period: end date in the past, not after the start date, year outside 1-9999 (code invalidContractPeriod)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(
                    implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "428",
            description = "If-Match is missing or is the wildcard * (code preconditionRequired)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(
                    implementation = ProblemDetail.class))
    )
    @PutMapping(ContractEndpoints.RELATIVE_CONTRACT_PERIOD)
    public ResponseEntity<Void> updateContractPeriod(
            @PathVariable final UUID contractId,
            @Parameter(in = ParameterIn.HEADER, required = true,
                    description = "ETag of the contract as read (GET). Wildcard * is not accepted.") @RequestHeader(
                            name = HttpHeaders.IF_MATCH, required = false) @Nullable final String ifMatch,
            @Valid @RequestBody final Request request
    ) {
        updateContractPeriod.execute(new UpdateContractPeriod.Command(
                contractId,
                EntityTags.requiredVersion(ifMatch),
                request.startDate(),
                request.endDate()
        ));
        return ResponseEntity.noContent().build();
    }

    public record Request(
            @NotNull(message = "Start date is required") @Schema(description = "Contract start date",
                    example = "2025-01-01T00:00:00", format = "date-time",
                    requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startDate,

            @Schema(description = "Contract end date (null or omitted: open-ended contract)",
                    example = "2027-01-01T00:00:00", format = "date-time",
                    nullable = true) @Nullable LocalDateTime endDate
    ) {
    }
}
