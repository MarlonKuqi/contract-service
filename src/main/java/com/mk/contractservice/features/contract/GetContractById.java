package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.contract.ContractDetails;
import com.mk.contractservice.domain.contract.ContractQueryPort;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface GetContractById {

    record Query(UUID contractId) {
        public Query {
            Objects.requireNonNull(contractId, "Contract ID cannot be null");
        }
    }

    ContractDetails execute(Query query);

    @UseCase
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    class Handler implements GetContractById {

        private final ContractQueryPort contractQueryPort;
        private final Clock clock;

        @Override
        public ContractDetails execute(final Query query) {
            return contractQueryPort.getDetails(query.contractId(), LocalDateTime.now(clock));
        }
    }
}

