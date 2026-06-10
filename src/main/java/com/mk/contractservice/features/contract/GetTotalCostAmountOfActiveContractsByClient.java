package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientValidationService;
import com.mk.contractservice.domain.contract.ContractQueryPort;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

        private final ClientValidationService clientValidationService;
        private final ContractQueryPort contractQueryPort;

        @Override
        public BigDecimal execute(final Query query) {
            clientValidationService.ensureClientExists(query.clientId());
            return contractQueryPort.calculateTotalCostAmountForClient(query.clientId());
        }
    }
}
