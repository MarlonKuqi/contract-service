package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.contract.usecase.UpdateContractCostUseCase;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.service.ContractService;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class UpdateContractCostUseCaseImpl implements UpdateContractCostUseCase {

    ContractRepository contractRepository;
    ContractService contractService;

    @Override
    public Contract execute(UpdateContractCostCommand command) {
        final Contract contract = contractService.getContractForClient(
                command.clientId(),
                command.contractId()
        );
        final ContractCost newCost = ContractCost.of(command.newCostAmount());
        final Contract updatedContract = contract.changeCost(newCost);
        return contractRepository.save(updatedContract);
    }
}

