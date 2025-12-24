package com.mk.contractservice.web.client.mapper;

import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.web.client.dto.CompanyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyResponseMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", expression = "java(company.getName().getValue())")
    @Mapping(target = "email", expression = "java(company.getEmail().getValue())")
    @Mapping(target = "phone", expression = "java(company.getPhone().getValue())")
    @Mapping(target = "companyIdentifier", expression = "java(company.getCompanyIdentifier().getValue())")
    CompanyResponse toDto(Company company);
}
