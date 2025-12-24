package com.mk.contractservice.web.client.mapper;

import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.web.client.dto.PersonResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PersonResponseMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", expression = "java(person.getName().getValue())")
    @Mapping(target = "email", expression = "java(person.getEmail().getValue())")
    @Mapping(target = "phone", expression = "java(person.getPhone().getValue())")
    @Mapping(target = "birthDate", expression = "java(person.getBirthDate().getValue())")
    PersonResponse toDto(Person person);
}
