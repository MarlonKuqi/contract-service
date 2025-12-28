package com.mk.contractservice.application.feature.contract.create;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.service.ContractValidationService;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public sealed interface CreateContract permits CreateContract.Handler {

    record Command(
            UUID clientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal costAmount
    ) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(startDate, "Start date cannot be null");
            Objects.requireNonNull(costAmount, "Cost amount cannot be null");
        }
    }

    Contract execute(Command command);

    @Service
    @Transactional
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    non-sealed class Handler implements CreateContract {

        ContractValidationService contractValidationService;
        ContractRepository contractRepository;

        @Override
        public Contract execute(final Command command) {
            contractValidationService.ensureClientExists(command.clientId());

            final ContractPeriod period = ContractPeriod.of(command.startDate(), command.endDate());
            final ContractCost cost = ContractCost.of(command.costAmount());

            final Contract contract = Contract.of(command.clientId(), period, cost);
            return contractRepository.save(contract);
        }
    }
}

