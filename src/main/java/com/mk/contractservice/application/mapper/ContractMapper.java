package com.mk.contractservice.application.mapper;

import com.mk.contractservice.application.dto.ContractDto;
import com.mk.contractservice.domain.contract.Contract;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    default ContractDto toDto(Contract contract) {
        return new ContractDto(
                contract.getId(),
                contract.getClient().getId(),
                contract.getPeriod().startDate(),
                contract.getPeriod().endDate(),
                contract.getPeriod().isActive(),
                contract.getCostAmount().value()
        );
    }
}

