package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractCoverage;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.domain.lamal.LamalAffiliations;
import com.mk.contractservice.features.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public interface CreateContract {

    record Command(
            UUID clientId,
            ContractType type,
            @Nullable Integer franchise,
            @Nullable LocalDateTime startDate,
            @Nullable LocalDateTime endDate,
            BigDecimal costAmount
    ) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(type, "Contract type cannot be null");
            Objects.requireNonNull(costAmount, "Cost amount cannot be null");
        }
    }

    Contract execute(Command command);

    @UseCase
    @RequiredArgsConstructor
    class Handler implements CreateContract {

        private final ClientRepository clientRepository;
        private final ContractRepository contractRepository;
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
            final LocalDateTime now = LocalDateTime.now(clock);
            final ContractPeriod period = ContractPeriod.of(command.startDate(), command.endDate(), now);
            final ContractCost cost = ContractCost.of(command.costAmount());
            final ContractCoverage coverage = ContractCoverage.of(command.type(), command.franchise());

            final Client client = clientRepository.getById(command.clientId());

            final Contract contract = Contract.of(client.id(), client.contractHolder(), coverage, period, cost, now);
            LamalAffiliations.of(lamalAffiliationRepository.loadForAdmission(Set.of(command.clientId())))
                    .admit(contract, now);
            return contractRepository.save(contract);
        }
    }
}
