package com.mk.contractservice.web.dto.contract;

import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;

import java.time.OffsetDateTime;

public record CreateContractResponse(
        ContractPeriod contractPeriod,
        ContractCost costAmount
) {
}
