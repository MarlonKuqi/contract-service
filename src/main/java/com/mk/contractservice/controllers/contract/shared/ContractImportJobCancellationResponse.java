package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.features.contract.importjob.CancelContractImportJob;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of the cancellation of an import job")
public record ContractImportJobCancellationResponse(
        @Schema(description = "The cancelled job") ContractImportJobResponse job,

        @Schema(description = "Number of contracts of the job closed by this call, 0 when the job was already cancelled",
                example = "498") int closedContracts
) {

    public static ContractImportJobCancellationResponse from(final CancelContractImportJob.Result result) {
        return new ContractImportJobCancellationResponse(ContractImportJobResponse.from(result.job()),
                result.closedContracts());
    }
}
