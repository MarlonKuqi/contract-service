package com.mk.contractservice.web.dto.contract;

import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;

import java.util.UUID;

public record ContractResponse(
        UUID id,
        UUID clientId,
        ContractPeriod period,
        ContractCost costAmount
) {
}
