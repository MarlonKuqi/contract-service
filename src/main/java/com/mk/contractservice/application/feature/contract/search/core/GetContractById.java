package com.mk.contractservice.application.feature.contract.search.core;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public sealed interface GetContractById permits GetContractById.Handler {

    record Query(UUID contractId, UUID clientId) {
        public Query {
            Objects.requireNonNull(contractId, "Contract ID cannot be null");
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    Contract execute(Query query);

    @Service
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    non-sealed class Handler implements GetContractById {

        ContractService contractService;

        @Override
        public Contract execute(final Query query) {
            return contractService.getContractForClient(
                    query.clientId(),
                    query.contractId()
            );
        }
    }
}

