package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.contract.usecase.CreateContractUseCase;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.service.ContractValidationService;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class CreateContractUseCaseImpl implements CreateContractUseCase {

    ContractValidationService contractValidationService;
    ContractRepository contractRepository;

    @Override
    public Contract execute(CreateContractCommand command) {
        final ContractPeriod period = ContractPeriod.of(command.startDate(), command.endDate());
        final ContractCost cost = ContractCost.of(command.costAmount());

        contractValidationService.ensureClientExists(command.clientId());

        final Contract contract = Contract.of(command.clientId(), period, cost);
        return contractRepository.save(contract);
    }
}
