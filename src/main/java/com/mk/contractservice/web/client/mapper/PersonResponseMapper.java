package com.mk.contractservice.web.client.mapper;

import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.web.client.dto.PersonResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PersonResponseMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", expression = "java(person.getName().value())")
    @Mapping(target = "email", expression = "java(person.getEmail().value())")
    @Mapping(target = "phone", expression = "java(person.getPhone().value())")
    @Mapping(target = "birthDate", expression = "java(person.getBirthDate().value())")
    PersonResponse toDto(Person person);
}
