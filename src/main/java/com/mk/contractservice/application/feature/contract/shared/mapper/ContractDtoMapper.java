package com.mk.contractservice.application.feature.contract.shared.mapper;

import com.mk.contractservice.application.feature.contract.shared.response.ContractResponse;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContractDtoMapper {

    default ContractResponse toResponse(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getClientId(),
                contract.getPeriod().getStartDate(),
                contract.getPeriod().getEndDate(),
                contract.isActive(),
                contract.getCostAmount().getValue()
        );
    }
}

