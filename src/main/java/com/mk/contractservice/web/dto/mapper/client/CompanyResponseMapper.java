package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.application.dto.CompanyDto;
import com.mk.contractservice.web.dto.client.CompanyResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyResponseMapper {
    CompanyResponse toDto(CompanyDto dto);
}