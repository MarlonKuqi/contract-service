package com.mk.contractservice.infrastructure.persistence.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportCancellation;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobLine;
import com.mk.contractservice.domain.contract.importjob.ContractImportProgress;
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
        final ContractImportSubmission submission = domain.getSubmission();
        final IdempotencyKey idempotencyKey = submission.getIdempotencyKey();
        final ContractImportJobJpaEntity entity = new ContractImportJobJpaEntity(
                submission.getFileName(),
                domain.getProgress().getTotalLines(),
                submission.getSubmittedAt(),
                submission.getSubmittedBy(),
                idempotencyKey == null ? null : idempotencyKey.getValue(),
                submission.getContentSha256()
        );
        mergeIntoExisting(domain, entity);
        return entity;
    }

    public static void mergeIntoExisting(final ContractImportJob domain, final ContractImportJobJpaEntity existing) {
        final ContractImportProgress progress = domain.getProgress();
        final ContractImportTermination termination = domain.getTermination();
        final ContractImportCancellation cancellation = domain.getCancellation();
        existing.setStatus(domain.getStatus());
        existing.setProcessedLines(progress.getProcessedLines());
        existing.setImportedCount(progress.getImportedCount());
        existing.setRejectedCount(progress.getRejectedCount());
        existing.setLeaseUntil(domain.getLeaseUntil());
        existing.setFinishedAt(termination == null ? null : termination.getFinishedAt());
        existing.setFailureReason(termination == null ? null : termination.getFailureReason());
        existing.setCancelledAt(cancellation == null ? null : cancellation.getCancelledAt());
        existing.setCancelledBy(cancellation == null ? null : cancellation.getCancelledBy());
    }

    public static ContractImportJobLineJpaEntity toLineEntity(final ContractImportJobLine line) {
        return new ContractImportJobLineJpaEntity(
                new ContractImportJobLineJpaEntity.LineId(line.getJobId(), line.getLineNumber()),
                line.getClientId(),
                line.getContractId(),
                line.getRejectionReason(),
                line.getRejectionMessage()
        );
    }

    public static ContractImportJobLine toDomainRejection(final ContractImportJobLineJpaEntity line) {
        return ContractImportJobLine.rejected(
                line.getId().getJobId(),
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
