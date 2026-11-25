package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.shared.RepositoryPort;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RepositoryPort
public interface ContractImportJobRepository {

    ContractImportJob save(ContractImportJob job, byte[] fileContent);

    ContractImportJob save(ContractImportJob job);

    ContractImportJob getById(UUID jobId);

    void ensureExists(UUID jobId);

    ContractImportJob lockById(UUID jobId);

    Optional<ContractImportJob> findByIdempotencyKey(String submittedBy, IdempotencyKey idempotencyKey);

    Page<ContractImportJob> findAll(@Nullable String submittedBy, Pageable pageable);

    Optional<ContractImportJob> findNextToRun(Instant now);

    byte[] loadFile(UUID jobId);

    void saveLines(List<ContractImportJobLine> lines);
}
