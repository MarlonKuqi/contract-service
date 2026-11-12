package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Contract to create")
public record ContractCreationRequest(
        @NotNull(message = "Client ID is required") @Schema(description = "Client ID (owner of the contract)",
                example = "550e8400-e29b-41d4-a716-446655440000",
                requiredMode = Schema.RequiredMode.REQUIRED) UUID clientId,

        @NotNull(message = "Contract type is required") @Schema(
                description = "Contract type. LAMAL (health insurance) is reserved to persons, LAA (accident insurance) and LPP (occupational pension) to companies",
                example = "LAMAL",
                requiredMode = Schema.RequiredMode.REQUIRED) ContractType type,

        @Schema(description = "LAMal franchise in CHF, required for LAMAL and forbidden for LAA and LPP. Adult: 300, 500, 1000, 1500, 2000 or 2500. Child (until December 31st of the year they turn 18): 0 to 600 by steps of 100, the contract ending at the latest on January 1st of the year they turn 19",
                example = "300") @Nullable Integer franchise,

        @Schema(description = "Contract start date (defaults to now if not provided)",
                example = "2025-01-01T00:00:00",
                format = "date-time") LocalDateTime startDate,

        @Schema(description = "Contract end date (null means active/indefinite contract)",
                example = "2026-01-01T00:00:00",
                format = "date-time") LocalDateTime endDate,

        @NotNull(message = "Cost amount is required") @Digits(integer = ContractCost.MAX_INTEGER_DIGITS,
                fraction = ContractCost.SCALE,
                message = "Cost amount must have at most " + ContractCost.MAX_INTEGER_DIGITS + " integer digits and " + ContractCost.SCALE + " decimal places") @Schema(
                        description = "Contract cost amount (must be positive, max " + ContractCost.MAX_INTEGER_DIGITS + " integer digits + " + ContractCost.SCALE + " decimals)",
                        example = "1500.00",
                        minimum = "0.01",
                        requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal costAmount
) {
}
