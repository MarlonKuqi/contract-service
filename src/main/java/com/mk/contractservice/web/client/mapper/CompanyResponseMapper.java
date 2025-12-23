package com.mk.contractservice.web.client.mapper;

import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.web.client.dto.CompanyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyResponseMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", expression = "java(company.getName().value())")
    @Mapping(target = "email", expression = "java(company.getEmail().value())")
    @Mapping(target = "phone", expression = "java(company.getPhone().value())")
    @Mapping(target = "companyIdentifier", expression = "java(company.getCompanyIdentifier().value())")
    CompanyResponse toDto(Company company);
}
