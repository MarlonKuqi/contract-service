package com.mk.contractservice.infrastructure.persistence.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportCancellation;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobLine;
import com.mk.contractservice.domain.contract.importjob.ContractImportProgress;
import com.mk.contractservice.domain.contract.importjob.ContractImportRejection;
import com.mk.contractservice.domain.contract.importjob.ContractImportSubmission;
import com.mk.contractservice.domain.contract.importjob.ContractImportTermination;
import com.mk.contractservice.domain.contract.importjob.IdempotencyKey;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractImportJobJpaMapper {

    public static ContractImportJob toDomain(final ContractImportJobJpaEntity entity) {
        return ContractImportJob.reconstituteFromDatabase(
                entity.getId(),
                submissionOf(entity),
                entity.getStatus(),
                progressOf(entity),
                entity.getLeaseUntil(),
                terminationOf(entity),
                cancellationOf(entity)
        );
    }

    public static ContractImportJobJpaEntity toNewEntity(final ContractImportJob domain) {
        final ContractImportSubmission submission = domain.submission();
        final IdempotencyKey idempotencyKey = submission.idempotencyKey();
        final ContractImportJobJpaEntity entity = new ContractImportJobJpaEntity(
                submission.fileName(),
                domain.progress().totalLines(),
                submission.submittedAt(),
                submission.submittedBy(),
                idempotencyKey == null ? null : idempotencyKey.value(),
                submission.contentSha256()
        );
        mergeIntoExisting(domain, entity);
        return entity;
    }

    public static void mergeIntoExisting(final ContractImportJob domain, final ContractImportJobJpaEntity existing) {
        final ContractImportProgress progress = domain.progress();
        final ContractImportTermination termination = domain.termination();
        final ContractImportCancellation cancellation = domain.cancellation();
        existing.setStatus(domain.status());
        existing.setProcessedLines(progress.processedLines());
        existing.setImportedCount(progress.importedCount());
        existing.setRejectedCount(progress.rejectedCount());
        existing.setLeaseUntil(domain.leaseUntil());
        existing.setFinishedAt(termination == null ? null : termination.finishedAt());
        existing.setFailureReason(termination == null ? null : termination.failureReason());
        existing.setCancelledAt(cancellation == null ? null : cancellation.cancelledAt());
        existing.setCancelledBy(cancellation == null ? null : cancellation.cancelledBy());
    }

    public static ContractImportJobLineJpaEntity toLineEntity(final ContractImportJobLine line) {
        return new ContractImportJobLineJpaEntity(
                new ContractImportJobLineJpaEntity.LineId(line.jobId(), line.lineNumber()),
                line.clientId(),
                line.contractId(),
                line.rejectionReason(),
                line.rejectionMessage()
        );
    }

    public static ContractImportRejection toDomainRejection(final ContractImportJobLineJpaEntity line) {
        return ContractImportRejection.reconstituteFromDatabase(
                line.getId().getLineNumber(),
                line.getClientId(),
                line.getRejectionReason(),
                line.getRejectionMessage()
        );
    }

    private static ContractImportSubmission submissionOf(final ContractImportJobJpaEntity entity) {
        return ContractImportSubmission.reconstituteFromDatabase(
                entity.getFileName(),
                entity.getSubmittedBy(),
                idempotencyKeyOf(entity),
                entity.getContentSha256(),
                entity.getCreatedAt()
        );
    }

    private static @Nullable IdempotencyKey idempotencyKeyOf(final ContractImportJobJpaEntity entity) {
        final String idempotencyKey = entity.getIdempotencyKey();
        return idempotencyKey == null ? null : IdempotencyKey.reconstituteFromDatabase(idempotencyKey);
    }

    private static ContractImportProgress progressOf(final ContractImportJobJpaEntity entity) {
        return ContractImportProgress.reconstituteFromDatabase(
                entity.getTotalLines(),
                entity.getProcessedLines(),
                entity.getImportedCount(),
                entity.getRejectedCount()
        );
    }

    private static @Nullable ContractImportTermination terminationOf(final ContractImportJobJpaEntity entity) {
        final LocalDateTime finishedAt = entity.getFinishedAt();
        return finishedAt == null
                ? null
                : ContractImportTermination.reconstituteFromDatabase(finishedAt, entity.getFailureReason());
    }

    private static @Nullable ContractImportCancellation cancellationOf(final ContractImportJobJpaEntity entity) {
        final LocalDateTime cancelledAt = entity.getCancelledAt();
        return cancelledAt == null
                ? null
                : ContractImportCancellation.reconstituteFromDatabase(cancelledAt, entity.getCancelledBy());
    }
}
