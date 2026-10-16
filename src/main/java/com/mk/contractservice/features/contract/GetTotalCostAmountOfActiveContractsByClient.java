package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.ContractQueryPort;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface GetTotalCostAmountOfActiveContractsByClient {

    record Query(UUID clientId) {
        public Query {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    BigDecimal execute(Query query);

    @UseCase
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    class Handler implements GetTotalCostAmountOfActiveContractsByClient {

        private final ClientRepository clientRepository;
        private final ContractQueryPort contractQueryPort;
        private final Clock clock;

        @Override
        public BigDecimal execute(final Query query) {
            clientRepository.ensureExists(query.clientId());
            return contractQueryPort.calculateTotalCostAmountForClient(query.clientId(), LocalDateTime.now(clock));
        }
    }
}
