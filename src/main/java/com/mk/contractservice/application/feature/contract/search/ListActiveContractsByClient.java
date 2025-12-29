package com.mk.contractservice.application.feature.contract.search;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface ListActiveContractsByClient {

    record Query(
            UUID clientId,
            Optional<LocalDateTime> updatedSince,
            Pageable pageable
    ) {
        public Query {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(updatedSince, "Updated since Optional cannot be null");
            Objects.requireNonNull(pageable, "Pageable cannot be null");
        }
    }

    Page<Contract> execute(Query query);

    @Service
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    class Handler implements ListActiveContractsByClient {

        ContractService contractService;

        @Override
        public Page<Contract> execute(final Query query) {
            return contractService.getActiveContractsForClient(
                    query.clientId(),
                    query.updatedSince().orElse(null),
                    query.pageable()
            );
        }
    }
}


