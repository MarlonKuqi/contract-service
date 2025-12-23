package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.contract.usecase.GetContractByIdQuery;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class GetContractByIdQueryImpl implements GetContractByIdQuery {

    ContractService contractService;

    @Override
    public Contract execute(GetContractQuery query) {
        return contractService.getContractForClient(query.clientId(), query.contractId());
    }
}

