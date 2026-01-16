package com.mk.contractservice.application.feature.contract.search;

import com.mk.contractservice.domain.client.service.ClientValidationService;
import com.mk.contractservice.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public interface GetTotalActiveContractsByClient {

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
    class Handler implements GetTotalActiveContractsByClient {

        ClientValidationService clientValidationService;
        ContractService contractService;

        @Override
        public BigDecimal execute(final Query query) {
            clientValidationService.ensureClientExists(query.clientId());
            return contractService.sumActiveContractsForClient(query.clientId());
        }
    }
}

