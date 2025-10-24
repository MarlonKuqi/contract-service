package com.mk.contractservice.web.dto.mapper.contract;

import com.mk.contractservice.domain.contract.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CreateContractMapper {

    @Mapping(target = "type", constant = "PERSON")
    @Mapping(target = "name", expression = "java(p.getName().value())")
    @Mapping(target = "email", expression = "java(p.getEmail().value())")
    @Mapping(target = "phone", expression = "java(p.getPhone().value())")
    @Mapping(target = "birthDate", source = "birthDate")
    @Mapping(target = "companyIdentifier", ignore = true)
    Contract toEntity(final CreateContrat p);
}
