package com.mk.contractservice.application.feature.contract.patchcost;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.service.ContractService;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public sealed interface PatchContractCost permits PatchContractCost.Handler {

    record Command(
            UUID clientId,
            UUID contractId,
            BigDecimal newCostAmount
    ) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(contractId, "Contract ID cannot be null");
            Objects.requireNonNull(newCostAmount, "New cost amount cannot be null");
        }
    }

    Contract execute(Command command);

    @Service
    @Transactional
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    non-sealed class Handler implements PatchContractCost {

        ContractRepository contractRepository;
        ContractService contractService;

        @Override
        public Contract execute(final Command command) {
            // Verify contract belongs to client (throws exception if not)
            final Contract contract = contractService.getContractForClient(
                    command.clientId(),
                    command.contractId()
            );

            final ContractCost newCost = ContractCost.of(command.newCostAmount());

            final Contract updatedContract = contract.changeCost(newCost);

            return contractRepository.save(updatedContract);
        }
    }
}

