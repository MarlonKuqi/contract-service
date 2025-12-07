package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.application.dto.PersonDto;
import com.mk.contractservice.web.dto.client.PersonResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PersonResponseMapper {
    PersonResponse toDto(final PersonDto dto);
}