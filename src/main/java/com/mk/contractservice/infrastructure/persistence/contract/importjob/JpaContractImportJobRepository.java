package com.mk.contractservice.infrastructure.persistence.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobLine;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobStatus;
import com.mk.contractservice.domain.contract.importjob.IdempotencyKey;
import com.mk.contractservice.domain.contract.importjob.exception.ContractImportJobNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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
    public ContractImportJob save(final ContractImportJob job, final byte[] fileContent) {
        final ContractImportJobJpaEntity entity = ContractImportJobJpaMapper.toNewEntity(job);
        entityManager.persist(entity);
        entityManager.persist(new ContractImportJobFileJpaEntity(entity.getId(), fileContent));
        return ContractImportJobJpaMapper.toDomain(entity);
    }

    @Override
    public ContractImportJob save(final ContractImportJob job) {
        final UUID jobId = job.id();
        if (jobId == null) {
            throw new IllegalStateException("Import job must be saved with its file before it is saved alone");
        }
        final ContractImportJobJpaEntity entity = jobJpaRepository.findById(jobId).orElseThrow(
                () -> new IllegalStateException("Import job with id " + jobId + " not found in database")
        );
        ContractImportJobJpaMapper.mergeIntoExisting(job, entity);
        return ContractImportJobJpaMapper.toDomain(entity);
    }

    @Override
    public ContractImportJob getById(final UUID jobId) {
        return jobJpaRepository.findById(jobId)
                .map(ContractImportJobJpaMapper::toDomain)
                .orElseThrow(() -> new ContractImportJobNotFoundException(jobId));
    }

    @Override
    public void ensureExists(final UUID jobId) {
        if (!jobJpaRepository.existsById(jobId)) {
            throw new ContractImportJobNotFoundException(jobId);
        }
    }

    @Override
    public ContractImportJob lockById(final UUID jobId) {
        return jobJpaRepository.findForUpdate(jobId)
                .map(ContractImportJobJpaMapper::toDomain)
                .orElseThrow(() -> new ContractImportJobNotFoundException(jobId));
    }

    @Override
    public Optional<ContractImportJob> findByIdempotencyKey(final String submittedBy, final IdempotencyKey idempotencyKey) {
        return jobJpaRepository.findBySubmittedByAndIdempotencyKey(submittedBy, idempotencyKey.value())
                .map(ContractImportJobJpaMapper::toDomain);
    }

    @Override
    public Page<ContractImportJob> findAll(@Nullable final String submittedBy, final Pageable pageable) {
        final Page<ContractImportJobJpaEntity> page = submittedBy == null
                ? jobJpaRepository.findAll(pageable)
                : jobJpaRepository.findBySubmittedBy(submittedBy, pageable);
        return page.map(ContractImportJobJpaMapper::toDomain);
    }

    @Override
    public Optional<ContractImportJob> findNextToRun(final Instant now) {
        return jobJpaRepository.findToRun(ContractImportJobStatus.PENDING, ContractImportJobStatus.RUNNING, now, Limit.of(1))
                .stream()
                .findFirst()
                .map(ContractImportJobJpaMapper::toDomain);
    }

    @Override
    public byte[] loadFile(final UUID jobId) {
        return fileJpaRepository.findById(jobId)
                .map(ContractImportJobFileJpaEntity::getContent)
                .orElseThrow(() -> new IllegalStateException("No file stored for import job " + jobId));
    }

    @Override
    public void saveLines(final List<ContractImportJobLine> lines) {
        lines.forEach(line -> entityManager.persist(ContractImportJobJpaMapper.toLineEntity(line)));
    }
}
