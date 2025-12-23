package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.contract.usecase.SumActiveContractsQuery;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SumActiveContractsQueryImpl implements SumActiveContractsQuery {

    ContractRepository contractRepository;

    @Override
    public BigDecimal execute(SumActiveContractsQueryParams query) {
        return contractRepository.sumActiveByClientId(query.clientId());
    }
}

