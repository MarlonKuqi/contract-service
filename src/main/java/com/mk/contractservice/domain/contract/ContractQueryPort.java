package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.shared.RepositoryPort;

import java.math.BigDecimal;
import java.util.UUID;

@RepositoryPort
public interface ContractQueryPort {

    BigDecimal calculateTotalCostAmountForClient(UUID clientId);
}
