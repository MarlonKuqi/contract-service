package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public interface LoadContractImportJobFile {

    record Query(UUID jobId) {
        public Query {
            Objects.requireNonNull(jobId, "Job ID cannot be null");
        }
    }

    byte[] execute(Query query);

    @UseCase
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    class Handler implements LoadContractImportJobFile {

        private final ContractImportJobRepository jobRepository;

        @Override
        public byte[] execute(final Query query) {
            return jobRepository.loadFile(query.jobId());
        }
    }
}
