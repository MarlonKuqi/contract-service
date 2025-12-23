package com.mk.contractservice.application.contract.usecase;

import com.mk.contractservice.domain.contract.aggregate.Contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface CreateContractUseCase {

    Contract execute(CreateContractCommand command);

    record CreateContractCommand(
            UUID clientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal costAmount
    ) {
        public CreateContractCommand {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(startDate, "Start date cannot be null");
            Objects.requireNonNull(costAmount, "Cost amount cannot be null");
        }
    }
}
