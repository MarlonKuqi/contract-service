package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractOverlapPolicy;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.features.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface CreateContract {

    record Command(
            UUID clientId,
            ContractType type,
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
        private final ContractOverlapPolicy contractOverlapPolicy;
        private final Clock clock;

        @Override
        public Contract execute(final Command command) {
            final LocalDateTime now = LocalDateTime.now(clock);
            final ContractPeriod period = ContractPeriod.of(command.startDate(), command.endDate(), now);
            final ContractCost cost = ContractCost.of(command.costAmount());

            final Client client = clientRepository.getById(command.clientId());

            final Contract contract = Contract.of(client.id(), client.kind(), command.type(), period, cost, now);
            contractOverlapPolicy.ensureNoOverlap(contract, now);
            return contractRepository.save(contract);
        }
    }
}
