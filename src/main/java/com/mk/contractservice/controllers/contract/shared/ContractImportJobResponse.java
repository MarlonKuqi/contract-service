package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.domain.contract.importjob.ContractImportCancellation;
import com.mk.contractservice.domain.contract.importjob.ContractImportFailureReason;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobStatus;
import com.mk.contractservice.domain.contract.importjob.ContractImportProgress;
import com.mk.contractservice.domain.contract.importjob.ContractImportSubmission;
import com.mk.contractservice.domain.contract.importjob.ContractImportTermination;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Asynchronous import of a CSV file of contracts")
public record ContractImportJobResponse(
        @Schema(description = "Job identifier", example = "019a3c1e-7b2f-7c3a-9d41-5e8f2a6b1c0d")
        UUID jobId,

        @Schema(description = "PENDING, RUNNING, COMPLETED, FAILED or CANCELLED. Lines processed before a failure stay imported, until the job is cancelled", example = "RUNNING")
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
        @Nullable String failureCode,

        @Schema(description = "Username of the counselor who submitted the file, null for jobs submitted before it was recorded", example = "counselor.demo")
        @Nullable String submittedBy,

        @Schema(description = "Cancellation date, null if the job was not cancelled")
        @Nullable LocalDateTime cancelledAt,

        @Schema(description = "Username of the counselor who cancelled the job, null if it was not cancelled", example = "counselor.demo")
        @Nullable String cancelledBy
) {

    public static ContractImportJobResponse from(final ContractImportJob job) {
        final ContractImportSubmission submission = job.getSubmission();
        final ContractImportProgress progress = job.getProgress();
        final ContractImportTermination termination = job.getTermination();
        final ContractImportCancellation cancellation = job.getCancellation();
        return new ContractImportJobResponse(
                job.getId(),
                job.getStatus(),
                submission.getFileName(),
                progress.getTotalLines(),
                progress.getProcessedLines(),
                progress.getImportedCount(),
                progress.getRejectedCount(),
                submission.getSubmittedAt(),
                termination == null ? null : termination.getFinishedAt(),
                failureCodeOf(termination),
                submission.getSubmittedBy(),
                cancellation == null ? null : cancellation.getCancelledAt(),
                cancellation == null ? null : cancellation.getCancelledBy()
        );
    }

    private static @Nullable String failureCodeOf(@Nullable final ContractImportTermination termination) {
        if (termination == null) {
            return null;
        }
        final ContractImportFailureReason reason = termination.getFailureReason();
        return reason == null ? null : reason.getCode();
    }
}
