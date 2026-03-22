package com.mk.contractservice.controllers.contract.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Contract details with period and cost information")
public record ContractResponse(
        @JsonProperty("id")
        @Schema(description = "Unique contract identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @JsonProperty("clientId")
        @Schema(description = "Client identifier owning this contract (null if contract is closed)",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                nullable = true)
        @Nullable UUID clientId,

        @JsonProperty("startDate")
        @Schema(description = "Contract start date", example = "2024-01-01T00:00:00")
        LocalDateTime startDate,

        @JsonProperty("endDate")
        @Schema(description = "Contract end date (null if still active)", example = "2024-12-31T23:59:59", nullable = true)
        @Nullable LocalDateTime endDate,

        @JsonProperty("active")
        @Schema(description = "Whether the contract is currently active", example = "true")
        boolean active,

        @JsonProperty("costAmount")
        @Schema(description = "Contract cost amount", example = "1500.00")
        BigDecimal costAmount
) {
}




