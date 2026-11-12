package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJobQueryPort;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportRejection;
import com.mk.contractservice.features.shared.PageBounds;
import com.mk.contractservice.features.shared.QueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Objects;
import java.util.UUID;

public interface ListContractImportJobRejections {

    int MAX_PAGE_SIZE = 100;

    record Query(UUID jobId, int page, int size) {
        public Query {
            Objects.requireNonNull(jobId, "Job ID cannot be null");
            PageBounds.requireValidPage(page);
            PageBounds.requireValidSize(size, MAX_PAGE_SIZE);
        }

        Pageable pageable() {
            return PageRequest.of(page, size);
        }
    }

    Page<ContractImportRejection> execute(Query query);

    @QueryUseCase
    @RequiredArgsConstructor
    class Handler implements ListContractImportJobRejections {

        private final ContractImportJobRepository jobRepository;
        private final ContractImportJobQueryPort queryPort;

        @Override
        public Page<ContractImportRejection> execute(final Query query) {
            jobRepository.ensureExists(query.jobId());
            return queryPort.findRejections(query.jobId(), query.pageable());
        }
    }
}
