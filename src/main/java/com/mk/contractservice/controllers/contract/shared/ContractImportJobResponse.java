package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Asynchronous import of a CSV file of contracts")
public record ContractImportJobResponse(
        @Schema(description = "Job identifier", example = "019a3c1e-7b2f-7c3a-9d41-5e8f2a6b1c0d")
        UUID jobId,

        @Schema(description = "PENDING, RUNNING, COMPLETED or FAILED. Lines processed before a failure stay imported", example = "RUNNING")
        ContractImportJobStatus status,

        @Schema(description = "Name of the submitted file", example = "contracts.csv")
        String fileName,

        @Schema(description = "Number of data lines in the file", example = "1200")
        int totalLines,

        @Schema(description = "Number of lines already processed", example = "500")
        int processedLines,

        @Schema(description = "Number of contracts created", example = "498")
        int importedCount,

        @Schema(description = "Number of lines rejected", example = "2")
        int rejectedCount,

        @Schema(description = "Submission date")
        LocalDateTime createdAt,

        @Schema(description = "End of the processing, null while it runs")
        LocalDateTime finishedAt,

        @Schema(description = "Why the job failed, null otherwise: unexpectedError, incompleteProcessing or tooManyAttempts", example = "unexpectedError")
        @Nullable String failureCode
) {

    public static ContractImportJobResponse from(final ContractImportJob job) {
        return new ContractImportJobResponse(
                job.getId(),
                job.getStatus(),
                job.getFileName(),
                job.getTotalLines(),
                job.getProcessedLines(),
                job.getImportedCount(),
                job.getRejectedCount(),
                job.getCreatedAt(),
                job.getFinishedAt(),
                job.getFailureReason() == null ? null : job.getFailureReason().getCode()
        );
    }
}
