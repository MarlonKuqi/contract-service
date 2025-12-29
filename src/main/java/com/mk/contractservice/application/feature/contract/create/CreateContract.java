package com.mk.contractservice.application.feature.contract.create;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.factory.ContractFactory;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.service.ContractValidationService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Service
    @Transactional
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    class Handler implements CreateContract {

        ContractValidationService contractValidationService;
        ContractRepository contractRepository;

        @Override
        public Contract execute(final Command command) {
            contractValidationService.ensureClientExists(command.clientId());

            final Contract contract = ContractFactory.create(
                    command.clientId(),
                    command.startDate(),
                    command.endDate(),
                    command.costAmount()
            );

            return contractRepository.save(contract);
        }
    }
}

