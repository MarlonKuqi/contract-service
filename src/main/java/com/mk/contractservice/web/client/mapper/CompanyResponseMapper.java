package com.mk.contractservice.web.client.mapper;

import com.mk.contractservice.application.client.dto.CompanyDto;
import com.mk.contractservice.web.client.dto.CompanyResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyResponseMapper {
    CompanyResponse toDto(CompanyDto dto);
}