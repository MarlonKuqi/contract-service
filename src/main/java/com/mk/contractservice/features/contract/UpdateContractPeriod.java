package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.domain.lamal.LamalAffiliations;
import com.mk.contractservice.domain.shared.ContractHolder;
import com.mk.contractservice.features.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
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
        private final ClientRepository clientRepository;
        private final LamalAffiliationRepository lamalAffiliationRepository;
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

            final ContractHolder holder = clientRepository.getContractHolderIncludingDeleted(
                    Objects.requireNonNull(contract.clientId()));
            final LocalDateTime now = LocalDateTime.now(clock);
            final Contract updatedContract = contract.changePeriod(
                    ContractPeriod.of(command.startDate(), command.endDate(), now),
                    holder,
                    now
            );

            if (updatedContract.equals(contract)) {
                return contract;
            }
            LamalAffiliations.of(lamalAffiliationRepository.loadForAdmission(Set.of(Objects.requireNonNull(
                    updatedContract.clientId()))))
                    .admit(updatedContract, now);
            return contractRepository.save(updatedContract);
        }
    }
}
