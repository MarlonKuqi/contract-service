package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientValidationService;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface ListActiveContractsByClient {

    record Query(
            UUID clientId,
            @Nullable LocalDateTime updatedSince,
            Pageable pageable
    ) {
        public Query {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(pageable, "Pageable cannot be null");
        }
    }

    Page<Contract> execute(Query query);

    @Service
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    class Handler implements ListActiveContractsByClient {

        private final ClientValidationService clientValidationService;
        private final ContractService contractService;

        @Override
        public Page<Contract> execute(final Query query) {
            clientValidationService.ensureClientExists(query.clientId());
            return contractService.getActiveContractsForClient(
                    query.clientId(),
                    query.updatedSince(),
                    query.pageable()
            );
        }
    }
}
