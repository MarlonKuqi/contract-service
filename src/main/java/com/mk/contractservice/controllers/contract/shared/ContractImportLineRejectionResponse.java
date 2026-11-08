package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.domain.contract.exception.ContractImportRejectedException.LineRejection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Rejected line of a synchronous import")
public record ContractImportLineRejectionResponse(
        @Schema(description = "Zero-based index of the line in the import", example = "1") int index,

        @Schema(description = "Client of the line") UUID clientId,

        @Schema(description = "invalidContractCost, invalidContractPeriod or clientNotFound",
                example = "invalidContractCost") String code,

        @Schema(description = "Reason of the rejection") String message
) {

    public static ContractImportLineRejectionResponse from(final LineRejection rejection) {
        return new ContractImportLineRejectionResponse(
                rejection.index(),
                rejection.clientId(),
                ContractImportCodes.of(rejection.reason()),
                rejection.message()
        );
    }
}
