package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public interface PatchContractCost {

    record Command(
            UUID contractId,
            BigDecimal newCostAmount
    ) {
        public Command {
            Objects.requireNonNull(contractId, "Contract ID cannot be null");
            Objects.requireNonNull(newCostAmount, "New cost amount cannot be null");
        }
    }

    Contract execute(Command command);

    @Service
    @Transactional
    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    class Handler implements PatchContractCost {

        ContractRepository contractRepository;

        @Override
        public Contract execute(final Command command) {
            final Contract contract = contractRepository.findById(command.contractId())
                    .orElseThrow(() -> new ContractNotFoundException(command.contractId()));

            final Contract updatedContract = contract.changeCost(ContractCost.of(command.newCostAmount()));

            return contractRepository.save(updatedContract);
        }
    }
}

