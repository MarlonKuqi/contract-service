package com.mk.contractservice.web.dto.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateContractRequest(
        @Schema(description = "Contract start date (defaults to now if not provided)", example = "2025-01-01T00:00:00", format = "date-time")
        LocalDateTime startDate,

        @Schema(description = "Contract end date (null means active/indefinite contract)", example = "2026-01-01T00:00:00", format = "date-time")
        LocalDateTime endDate,

        @NotNull(message = "Cost amount is required")
        @Positive(message = "Cost amount must positive and greater than zero")
        @Digits(integer = 12, fraction = 2, message = "Cost amount must have at most 12 digits and 2 decimal places")
        @Schema(description = "Contract cost amount (must be positive, max 12 digits + 2 decimals)", example = "1500.00", minimum = "0.01", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal costAmount
) {
}
