package com.mk.contractservice.application.feature.contract.search;

import com.mk.contractservice.domain.client.service.ClientValidationService;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public interface GetContractById {

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
    class Handler implements GetContractById {

        ClientValidationService clientValidationService;
        ContractService contractService;

        @Override
        public Contract execute(final Query query) {
            clientValidationService.ensureClientExists(query.clientId());
            return contractService.getContractForClient(
                    query.clientId(),
                    query.contractId()
            );
        }
    }
}

