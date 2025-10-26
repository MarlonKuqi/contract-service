package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.web.dto.client.CreatePersonResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PersonResponseMapper {
    CreatePersonResponse toDto(final Person p);
}