package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.contract.usecase.GetActiveContractsQuery;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class GetActiveContractsQueryImpl implements GetActiveContractsQuery {

    ContractRepository contractRepository;

    @Override
    public Page<Contract> execute(GetActiveContractsQueryParams query) {
        return contractRepository.findActiveByClientIdPageable(
                query.clientId(),
                query.updatedSince(),
                query.pageable()
        );
    }
}

