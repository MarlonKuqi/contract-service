package com.mk.contractservice.application.feature.contract.search.core;

import com.mk.contractservice.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public sealed interface SumActiveContractsByClient permits SumActiveContractsByClient.Handler {

    record Query(UUID clientId) {
        public Query {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    BigDecimal execute(Query query);

    @Service
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    non-sealed class Handler implements SumActiveContractsByClient {

        ContractService contractService;

        @Override
        public BigDecimal execute(final Query query) {
            return contractService.sumActiveContractsForClient(query.clientId());
        }
    }
}

