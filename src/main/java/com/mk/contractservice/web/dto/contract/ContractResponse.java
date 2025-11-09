package com.mk.contractservice.web.dto.contract;

import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Contract details with period and cost information")
public record ContractResponse(
        @Schema(description = "Unique contract identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Client identifier owning this contract", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID clientId,

        @Schema(description = "Contract period with active status and dates")
        ContractPeriod period,

        @Schema(description = "Contract cost amount", example = "1500.00")
        ContractCost costAmount
) {
}
