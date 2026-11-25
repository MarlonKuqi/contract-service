package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.features.shared.PageBounds;
import com.mk.contractservice.features.shared.QueryUseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface ListContractImportJobs {

    int MAX_PAGE_SIZE = 100;

    record Query(@Nullable String submittedBy, int page, int size) {
        public Query {
            PageBounds.requireValidPage(page);
            PageBounds.requireValidSize(size, MAX_PAGE_SIZE);
        }

        Pageable pageable() {
            return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        }
    }

    Page<ContractImportJob> execute(Query query);

    @QueryUseCase
    @RequiredArgsConstructor
    class Handler implements ListContractImportJobs {

        private final ContractImportJobRepository jobRepository;

        @Override
        public Page<ContractImportJob> execute(final Query query) {
            return jobRepository.findAll(query.submittedBy(), query.pageable());
        }
    }
}
