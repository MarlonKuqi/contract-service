package com.mk.contractservice.web.contract.mapper;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.web.contract.dto.ContractResponse;
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
