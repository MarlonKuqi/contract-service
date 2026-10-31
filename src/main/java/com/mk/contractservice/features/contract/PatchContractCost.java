package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface PatchContractCost {

    record Command(
            UUID contractId,
            long expectedVersion,
            BigDecimal newCostAmount
    ) {
        public Command {
            Objects.requireNonNull(contractId, "Contract ID cannot be null");
            Objects.requireNonNull(newCostAmount, "New cost amount cannot be null");
        }
    }

    Contract execute(Command command);

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements PatchContractCost {

        private final ContractRepository contractRepository;
        private final Clock clock;

        @Override
        @Retryable(
                includes = OptimisticLockingFailureException.class,
                maxRetries = 3,
                delay = 20,
                jitter = 10
        )
        public Contract execute(final Command command) {
            final Contract contract = contractRepository.getById(command.contractId());
            contractRepository.ensureVersion(command.contractId(), command.expectedVersion());

            final Contract updatedContract = contract.changeCost(
                    ContractCost.of(command.newCostAmount()),
                    LocalDateTime.now(clock)
            );

            return updatedContract == contract ? contract : contractRepository.save(updatedContract);
        }
    }
}

