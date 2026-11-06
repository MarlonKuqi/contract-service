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

        @Schema(description = "PENDING, RUNNING, COMPLETED, FAILED or CANCELLED. Lines processed before a failure stay imported, until the job is cancelled",
                example = "RUNNING")
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

        @Schema(description = "Why the job failed, null otherwise: unexpectedError, incompleteProcessing or tooManyAttempts",
                example = "unexpectedError")
        @Nullable String failureCode,

        @Schema(description = "Username of the counselor who submitted the file, null for jobs submitted before it was recorded",
                example = "counselor.demo")
        @Nullable String submittedBy,

        @Schema(description = "Cancellation date, null if the job was not cancelled")
        @Nullable LocalDateTime cancelledAt,

        @Schema(description = "Username of the counselor who cancelled the job, null if it was not cancelled",
                example = "counselor.demo")
        @Nullable String cancelledBy
) {

    public static ContractImportJobResponse from(final ContractImportJob job) {
        final ContractImportSubmission submission = job.submission();
        final ContractImportProgress progress = job.progress();
        final ContractImportTermination termination = job.termination();
        final ContractImportCancellation cancellation = job.cancellation();
        return new ContractImportJobResponse(
                job.id(),
                job.status(),
                submission.fileName(),
                progress.totalLines(),
                progress.processedLines(),
                progress.importedCount(),
                progress.rejectedCount(),
                submission.submittedAt(),
                termination == null ? null : termination.finishedAt(),
                failureCodeOf(termination),
                submission.submittedBy(),
                cancellation == null ? null : cancellation.cancelledAt(),
                cancellation == null ? null : cancellation.cancelledBy()
        );
    }

    private static @Nullable String failureCodeOf(@Nullable final ContractImportTermination termination) {
        return switch (termination) {
            case ContractImportTermination.Failed(LocalDateTime _, ContractImportFailureReason reason) -> ContractImportCodes.of(reason);
            case ContractImportTermination.Ended _ -> null;
            case null -> null;
        };
    }
}
