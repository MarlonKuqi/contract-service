package com.mk.contractservice.infrastructure.web.contract;

import com.mk.contractservice.application.feature.contract.PatchContractCost;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractEndpoints;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractSwaggerTags;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@Tag(name = ContractSwaggerTags.NAME, description = ContractSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ContractEndpoints.CONTRACTS_BASE)
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class PatchContractCostController {

    PatchContractCost patchContractCost;

    @Operation(
            summary = "Update the cost amount of a contract",
            description = "Updates only the costAmount field. The lastModified field is automatically updated internally. "
                    + "Business rule: Only active contracts (not expired) can be updated."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Cost updated successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input data (validation failed)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Contract not found",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "422",
            description = "Business validation failed (e.g., contract is expired and cannot be modified)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PatchMapping(ContractEndpoints.RELATIVE_CONTRACT_COST)
    public ResponseEntity<Void> patchContractCost(
            @PathVariable final UUID contractId,
            @Valid @RequestBody final Request request
    ) {
        patchContractCost.execute(new PatchContractCost.Command(
                contractId,
                request.amount()
        ));
        return ResponseEntity.noContent().build();
    }

    public record Request(
            @NotNull(message = "Amount is required")
            @Positive(message = "Cost amount must be positive and greater than zero")
            @Digits(integer = 12,
                    fraction = 2,
                    message = "Amount must have at most 12 digits and 2 decimal places")
            @Schema(description = "New cost amount (must be positive, max 12 digits + 2 decimals)",
                    example = "2500.00",
                    minimum = "0.01",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal amount
    ) {
    }
}

