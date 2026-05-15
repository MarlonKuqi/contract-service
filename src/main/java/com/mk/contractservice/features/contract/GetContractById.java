package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public interface GetContractById {

    record Query(UUID contractId) {
        public Query {
            Objects.requireNonNull(contractId, "Contract ID cannot be null");
        }
    }

    Contract execute(Query query);

    @Service
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    class Handler implements GetContractById {

        private final ContractRepository contractRepository;

        @Override
        public Contract execute(final Query query) {
            return contractRepository.findById(query.contractId())
                    .orElseThrow(() -> new ContractNotFoundException(query.contractId()));
        }
    }
}

