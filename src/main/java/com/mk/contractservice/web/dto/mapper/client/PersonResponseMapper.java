package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.web.dto.client.CreatePersonResponse;
import com.mk.contractservice.web.dto.mapper.common.ValueObjectMappers;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ValueObjectMappers.class)
public interface PersonResponseMapper {
    CreatePersonResponse toDto(final Person p);
}