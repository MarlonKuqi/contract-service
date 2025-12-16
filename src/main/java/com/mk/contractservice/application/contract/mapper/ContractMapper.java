package com.mk.contractservice.application.contract.mapper;

import com.mk.contractservice.application.contract.dto.ContractDto;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ContractValueObjectMappers.class)
public interface ContractMapper {

    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "startDate", source = "period", qualifiedByName = "periodToStartDate")
    @Mapping(target = "endDate", source = "period", qualifiedByName = "periodToEndDate")
    @Mapping(target = "active", expression = "java(contract.isActive())")
    @Mapping(target = "costAmount", source = "costAmount")
    ContractDto toDto(Contract contract);
}


