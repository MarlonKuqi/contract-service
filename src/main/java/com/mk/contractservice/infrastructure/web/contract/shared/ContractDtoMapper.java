package com.mk.contractservice.infrastructure.web.contract.shared;

import com.mk.contractservice.domain.contract.Contract;
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

