package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractOverlapPolicy;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.features.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface UpdateContractPeriod {

    record Command(
            UUID contractId,
            long expectedVersion,
            LocalDateTime startDate,
            @Nullable LocalDateTime endDate
    ) {
        public Command {
            Objects.requireNonNull(contractId, "Contract ID cannot be null");
            Objects.requireNonNull(startDate, "Start date cannot be null");
        }
    }

    Contract execute(Command command);

    @UseCase
    @RequiredArgsConstructor
    class Handler implements UpdateContractPeriod {

        private final ContractRepository contractRepository;
        private final ContractOverlapPolicy contractOverlapPolicy;
        private final Clock clock;

        @Override
        public Contract execute(final Command command) {
            final Contract contract = contractRepository.getById(command.contractId());
            contractRepository.ensureVersion(command.contractId(), command.expectedVersion());

            final LocalDateTime now = LocalDateTime.now(clock);
            final Contract updatedContract = contract.changePeriod(
                    ContractPeriod.of(command.startDate(), command.endDate(), now),
                    now
            );

            if (updatedContract.equals(contract)) {
                return contract;
            }
            contractOverlapPolicy.ensureNoOverlap(updatedContract, now);
            return contractRepository.save(updatedContract);
        }
    }
}
