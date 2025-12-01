package com.mk.contractservice.web.dto.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Created contract details (returned after successful creation)")
public record CreateContractResponse(
        @Nullable
        @JsonProperty("id")
        @Schema(description = "Contract unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @JsonProperty("period")
        @Schema(description = "Contract period with start and end dates")
        PeriodResponse period,

        @JsonProperty("costAmount")
        @Schema(description = "Contract cost amount", example = "1500.00")
        BigDecimal costAmount
) {
    @Schema(description = "Contract period information")
    public record PeriodResponse(
            @Nullable
            @JsonProperty("startDate")
            @Schema(description = "Contract start date", example = "2025-01-01T00:00:00")
            LocalDateTime startDate,

            @Nullable
            @JsonProperty("endDate")
            @Schema(description = "Contract end date (null for indefinite/active contracts)", example = "2026-01-01T00:00:00", nullable = true)
            LocalDateTime endDate
    ) {
    }
}
