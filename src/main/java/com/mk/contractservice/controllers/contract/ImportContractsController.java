package com.mk.contractservice.controllers.contract;

import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractImportResponse;
import com.mk.contractservice.controllers.contract.shared.ContractSwaggerTags;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.features.contract.ImportContracts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Tag(name = ContractSwaggerTags.NAME, description = ContractSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ContractEndpoints.CONTRACTS_BASE)
@RequiredArgsConstructor
public class ImportContractsController {

    private final ImportContracts importContracts;

    @Operation(
            summary = "Import contracts in bulk (all or nothing)",
            description = "Creates up to " + ImportContracts.MAX_LINES + " contracts in one transaction. "
                    + "If any line is rejected, nothing is created and the answer lists every rejected line: "
                    + "fix them and send the whole import again, without any risk of duplicates. "
                    + "An accepted import must not be sent again: it would create the contracts a second time."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Every contract was created",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContractImportResponse.class))
    )
    @ApiResponse(
            responseCode = "422",
            description = "Nothing was created. Code contractImportRejected: the rejections property lists each "
                    + "rejected line with its zero-based index, its clientId, its code (invalidContractCost, invalidContractPeriod, "
                    + "clientNotFound) and a message. Code invalidImportSize: empty or too many lines. "
                    + "Code validationError: missing field or wrong type, named as contracts[n].field",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error. Nothing was created",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping(ContractEndpoints.RELATIVE_CONTRACT_IMPORTS)
    public ResponseEntity<ContractImportResponse> importContracts(
            @Valid @RequestBody final Request request,
            final Locale locale
    ) {
        final List<Contract> contracts = importContracts.execute(new ImportContracts.Command(
                request.contracts().stream()
                        .map(line -> new ImportContracts.Line(
                                line.clientId(),
                                line.startDate(),
                                line.endDate(),
                                line.costAmount()))
                        .toList()
        ));

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(new ContractImportResponse(
                        contracts.size(),
                        contracts.stream().map(Contract::getId).toList()
                ));
    }

    public record Request(
            @NotNull(message = "Contracts are required")
            @Schema(description = "Contracts to create, at most " + ImportContracts.MAX_LINES,
                    requiredMode = Schema.RequiredMode.REQUIRED)
            List<@NotNull(message = "A line cannot be null") @Valid Line> contracts
    ) {
    }

    public record Line(
            @NotNull(message = "Client ID is required")
            @Schema(description = "Client ID (owner of the contract)",
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            UUID clientId,

            @Schema(description = "Contract start date (defaults to now if not provided)",
                    example = "2025-01-01T00:00:00",
                    format = "date-time")
            LocalDateTime startDate,

            @Schema(description = "Contract end date (null means active/indefinite contract)",
                    example = "2026-01-01T00:00:00",
                    format = "date-time")
            LocalDateTime endDate,

            @NotNull(message = "Cost amount is required")
            @Digits(integer = ContractCost.MAX_INTEGER_DIGITS,
                    fraction = ContractCost.SCALE,
                    message = "Cost amount must have at most " + ContractCost.MAX_INTEGER_DIGITS + " integer digits and " + ContractCost.SCALE + " decimal places")
            @Schema(description = "Contract cost amount (must be positive, max " + ContractCost.MAX_INTEGER_DIGITS + " integer digits + " + ContractCost.SCALE + " decimals)",
                    example = "1500.00",
                    minimum = "0.01",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal costAmount
    ) {
    }
}
