package com.mk.contractservice.web.dto.mapper.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.web.dto.contract.ContractResponse;
import com.mk.contractservice.web.dto.mapper.common.ValueObjectMappers;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ValueObjectMappers.class)
public interface ContractMapper {

    @Mapping(target = "clientId", expression = "java(c.getClient().getId())")
    ContractResponse toDto(Contract c);
}