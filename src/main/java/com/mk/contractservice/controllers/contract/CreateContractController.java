package com.mk.contractservice.controllers.contract;

import com.mk.contractservice.controllers.contract.shared.ContractDtoMapper;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractResponse;
import com.mk.contractservice.controllers.contract.shared.ContractSwaggerTags;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.features.contract.CreateContract;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Tag(name = ContractSwaggerTags.NAME, description = ContractSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ContractEndpoints.CONTRACTS_BASE)
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class CreateContractController {

    CreateContract createContract;
    ContractDtoMapper contractMapper;

    @Operation(
            summary = "Create a contract for a client",
            description = "Creates a new contract for a client. "
                    + "startDate defaults to now if not provided. "
                    + "endDate null means active contract."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Contract created successfully",
            headers = {
                    @Header(name = "Location", description = "URI of the created contract resource")
            },
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContractResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Malformed JSON / invalid syntax",
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
            description = "Business validation failed (e.g., invalid cost amount, date range)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping
    public ResponseEntity<ContractResponse> createContract(
            @Valid @RequestBody final Request request,
            final UriComponentsBuilder uriBuilder,
            final Locale locale
    ) {
        final Contract contract = createContract.execute(new CreateContract.Command(
                request.clientId(),
                request.startDate(),
                request.endDate(),
                request.costAmount()
        ));

        final var location = uriBuilder
                .path(ContractEndpoints.CONTRACT_BY_ID)
                .buildAndExpand(contract.getId())
                .toUri();

        final ContractResponse response = contractMapper.toResponse(contract);

        return ResponseEntity.created(location)
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(response);
    }

    public record Request(
            @NotNull(message = "Client ID is required")
            @Schema(description = "Client ID (owner of the contract)",
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            UUID clientId,

            @Schema(description = "Contract start date (defaults to now if not provided)", example = "2025-01-01T00:00:00", format = "date-time")
            LocalDateTime startDate,

            @Schema(description = "Contract end date (null means active/indefinite contract)", example = "2026-01-01T00:00:00", format = "date-time")
            LocalDateTime endDate,

            @NotNull(message = "Cost amount is required")
            @Positive(message = "Cost amount must be positive and greater than zero")
            @Digits(integer = 12,
                    fraction = 2,
                    message = "Cost amount must have at most 12 digits and 2 decimal places")
            @Schema(description = "Contract cost amount (must be positive, max 12 digits + 2 decimals)",
                    example = "1500.00",
                    minimum = "0.01",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal costAmount
    ) {
    }
}

