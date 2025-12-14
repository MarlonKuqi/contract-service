package com.mk.contractservice.web.client.mapper;

import com.mk.contractservice.application.client.dto.PersonDto;
import com.mk.contractservice.web.client.dto.PersonResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PersonResponseMapper {
    PersonResponse toDto(final PersonDto dto);
}