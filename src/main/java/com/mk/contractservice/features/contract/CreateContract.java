package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface CreateContract {

    record Command(
            UUID clientId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal costAmount
    ) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(costAmount, "Cost amount cannot be null");
        }
    }

    Contract execute(Command command);

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements CreateContract {

        private final ClientRepository clientRepository;
        private final ContractRepository contractRepository;
        private final Clock clock;

        @Override
        public Contract execute(final Command command) {
            final LocalDateTime now = LocalDateTime.now(clock);
            final Contract contract = Contract.of(
                    command.clientId(),
                    ContractPeriod.of(command.startDate(), command.endDate(), now),
                    ContractCost.of(command.costAmount()),
                    now
            );

            clientRepository.ensureExists(command.clientId());

            return contractRepository.save(contract);
        }
    }
}
