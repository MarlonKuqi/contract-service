package com.mk.contractservice.web.dto.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CostUpdateRequest(
        @NotNull(message = "Amount is required")
        @Positive(message = "Cost amount must positive and greater than zero")
        @Digits(integer = 12, fraction = 2, message = "Amount must have at most 12 digits and 2 decimal places")
        @Schema(description = "New cost amount (must be positive, max 12 digits + 2 decimals)", example = "2500.00", minimum = "0.01", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount
) {
}
