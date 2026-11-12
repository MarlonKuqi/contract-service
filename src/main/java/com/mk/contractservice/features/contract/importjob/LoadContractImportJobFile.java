package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.features.shared.QueryUseCase;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.UUID;

public interface LoadContractImportJobFile {

    record Query(UUID jobId) {
        public Query {
            Objects.requireNonNull(jobId, "Job ID cannot be null");
        }
    }

    byte[] execute(Query query);

    @QueryUseCase
    @RequiredArgsConstructor
    class Handler implements LoadContractImportJobFile {

        private final ContractImportJobRepository jobRepository;

        @Override
        public byte[] execute(final Query query) {
            return jobRepository.loadFile(query.jobId());
        }
    }
}
