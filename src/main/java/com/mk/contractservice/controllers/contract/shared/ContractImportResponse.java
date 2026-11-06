package com.mk.contractservice.controllers.contract.shared;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Result of an accepted contract import")
public record ContractImportResponse(
        @Schema(description = "Number of contracts created", example = "2")
        int importedCount,

        @Schema(description = "Ids of the created contracts, in the order of the submitted lines")
        List<UUID> contractIds
) {
}
