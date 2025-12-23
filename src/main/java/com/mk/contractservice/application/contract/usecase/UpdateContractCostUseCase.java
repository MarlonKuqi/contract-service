package com.mk.contractservice.application.contract.usecase;

import com.mk.contractservice.domain.contract.aggregate.Contract;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public interface UpdateContractCostUseCase {

    Contract execute(UpdateContractCostCommand command);

    record UpdateContractCostCommand(
            UUID clientId,
            UUID contractId,
            BigDecimal newCostAmount
    ) {
        public UpdateContractCostCommand {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(contractId, "Contract ID cannot be null");
            Objects.requireNonNull(newCostAmount, "New cost amount cannot be null");
        }
    }
}

