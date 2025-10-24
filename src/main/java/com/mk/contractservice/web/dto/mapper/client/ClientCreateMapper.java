package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.web.dto.client.CreateCompanyRequest;
import com.mk.contractservice.web.dto.client.CreatePersonRequest;
import com.mk.contractservice.web.dto.mapper.common.ValueObjectMappers;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ValueObjectMappers.class)
public interface ClientCreateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name", qualifiedByName = "toName")
    @Mapping(target = "email", source = "email", qualifiedByName = "toEmail")
    @Mapping(target = "phone", source = "phone", qualifiedByName = "toPhone")
    Person toEntity(final CreatePersonRequest req);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name", qualifiedByName = "toName")
    @Mapping(target = "email", source = "email", qualifiedByName = "toEmail")
    @Mapping(target = "phone", source = "phone", qualifiedByName = "toPhone")
    Company toEntity(final CreateCompanyRequest req);
}