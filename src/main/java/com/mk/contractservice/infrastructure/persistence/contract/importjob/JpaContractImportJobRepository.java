package com.mk.contractservice.infrastructure.persistence.contract.importjob;

import com.mk.contractservice.domain.contract.exception.ContractImportJobNotFoundException;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobLine;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaContractImportJobRepository implements ContractImportJobRepository {

    private final ContractImportJobJpaRepository jobJpaRepository;
    private final ContractImportJobFileJpaRepository fileJpaRepository;
    private final ContractImportJobLineJpaRepository lineJpaRepository;
    private final EntityManager entityManager;

    @Override
    public ContractImportJob insert(final ContractImportJob job, final byte[] fileContent) {
        final ContractImportJobJpaEntity entity = new ContractImportJobJpaEntity(job.getFileName(), job.getTotalLines(), job.getCreatedAt());
        mergeIntoExisting(job, entity);
        entityManager.persist(entity);
        entityManager.persist(new ContractImportJobFileJpaEntity(entity.getId(), fileContent));
        return toDomain(entity);
    }

    @Override
    public ContractImportJob save(final ContractImportJob job) {
        final UUID jobId = job.getId();
        final ContractImportJobJpaEntity entity = jobJpaRepository.findById(jobId).orElseThrow(
                () -> new IllegalStateException("Import job with id " + jobId + " not found in database")
        );
        mergeIntoExisting(job, entity);
        return toDomain(entity);
    }

    @Override
    public ContractImportJob getById(final UUID jobId) {
        return jobJpaRepository.findById(jobId)
                .map(JpaContractImportJobRepository::toDomain)
                .orElseThrow(() -> new ContractImportJobNotFoundException(jobId));
    }

    @Override
    public Optional<ContractImportJob> findNextToRun(final Instant now) {
        return jobJpaRepository.findToRun(ContractImportJobStatus.PENDING, ContractImportJobStatus.RUNNING, now, Limit.of(1))
                .stream()
                .findFirst()
                .map(JpaContractImportJobRepository::toDomain);
    }

    @Override
    public byte[] loadFile(final UUID jobId) {
        return fileJpaRepository.findById(jobId)
                .map(ContractImportJobFileJpaEntity::getContent)
                .orElseThrow(() -> new IllegalStateException("No file stored for import job " + jobId));
    }

    @Override
    public void insertLines(final List<ContractImportJobLine> lines) {
        lines.forEach(line -> entityManager.persist(new ContractImportJobLineJpaEntity(
                new ContractImportJobLineJpaEntity.LineId(line.getJobId(), line.getLineNumber()),
                line.getClientId(),
                line.getContractId(),
                line.getRejectionReason(),
                line.getRejectionMessage()
        )));
    }

    @Override
    public Page<ContractImportJobLine> findRejections(final UUID jobId, final Pageable pageable) {
        return lineJpaRepository.findRejections(jobId, pageable)
                .map(line -> ContractImportJobLine.rejected(
                        line.getId().getJobId(),
                        line.getId().getLineNumber(),
                        line.getClientId(),
                        line.getRejectionReason(),
                        line.getRejectionMessage()
                ));
    }

    private static void mergeIntoExisting(final ContractImportJob job, final ContractImportJobJpaEntity entity) {
        entity.setStatus(job.getStatus());
        entity.setProcessedLines(job.getProcessedLines());
        entity.setImportedCount(job.getImportedCount());
        entity.setRejectedCount(job.getRejectedCount());
        entity.setLeaseUntil(job.getLeaseUntil());
        entity.setFinishedAt(job.getFinishedAt());
        entity.setFailureReason(job.getFailureReason());
    }

    private static ContractImportJob toDomain(final ContractImportJobJpaEntity entity) {
        return ContractImportJob.reconstituteFromDatabase(
                entity.getId(),
                entity.getFileName(),
                entity.getStatus(),
                entity.getTotalLines(),
                entity.getProcessedLines(),
                entity.getImportedCount(),
                entity.getRejectedCount(),
                entity.getLeaseUntil(),
                entity.getCreatedAt(),
                entity.getFinishedAt(),
                entity.getFailureReason()
        );
    }
}
