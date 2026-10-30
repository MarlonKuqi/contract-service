package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractSearchCriteria;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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

    @UseCase
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    class Handler implements ListActiveContractsByClient {

        private final ClientRepository clientRepository;
        private final ContractRepository contractRepository;
        private final Clock clock;

        @Override
        public Page<Contract> execute(final Query query) {
            clientRepository.ensureExists(query.clientId());
            final ContractSearchCriteria criteria = ContractSearchCriteria.activeForClient(
                    query.clientId(),
                    LocalDateTime.now(clock),
                    query.updatedSince()
            );
            return contractRepository.findByCriteria(criteria, query.pageable());
        }
    }
}
