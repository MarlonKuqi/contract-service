package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.web.dto.client.CompanyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyResponseMapper {
    @Mapping(target = "name", expression = "java(c.getName().value())")
    @Mapping(target = "email", expression = "java(c.getEmail().value())")
    @Mapping(target = "phone", expression = "java(c.getPhone().value())")
    CompanyResponse toDto(Company c);
}