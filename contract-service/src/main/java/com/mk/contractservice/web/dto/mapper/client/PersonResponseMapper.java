package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.web.dto.client.CreatePersonResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PersonResponseMapper {
    @Mapping(target = "name", expression = "java(p.getName().value())")
    @Mapping(target = "email", expression = "java(p.getEmail().value())")
    @Mapping(target = "phone", expression = "java(p.getPhone().value())")
    CreatePersonResponse toDto(final Person p);
}