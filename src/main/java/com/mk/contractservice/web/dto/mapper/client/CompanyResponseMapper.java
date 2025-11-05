package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.web.dto.client.CreateCompanyResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyResponseMapper {
    CreateCompanyResponse toDto(Company c);
}