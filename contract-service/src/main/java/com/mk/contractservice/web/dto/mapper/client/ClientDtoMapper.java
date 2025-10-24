package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.web.dto.client.ClientResponse;
import com.mk.contractservice.web.dto.client.CompanyResponse;
import com.mk.contractservice.web.dto.client.PersonResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClientDtoMapper {

    @Mapping(target = "type", constant = "PERSON")
    @Mapping(target = "name", expression = "java(p.getName().value())")
    @Mapping(target = "email", expression = "java(p.getEmail().value())")
    @Mapping(target = "phone", expression = "java(p.getPhone().value())")
    @Mapping(target = "birthDate", source = "birthDate")
    @Mapping(target = "companyIdentifier", ignore = true)
    ClientResponse toResponse(final Person p);

    @Mapping(target = "type", constant = "COMPANY")
    @Mapping(target = "name", expression = "java(c.getName().value())")
    @Mapping(target = "email", expression = "java(c.getEmail().value())")
    @Mapping(target = "phone", expression = "java(c.getPhone().value())")
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "companyIdentifier", source = "companyIdentifier")
    ClientResponse toResponse(final Company c);

    default ClientResponse toResponse(final Client client) {
        return switch (client) {
            case final Person p -> new PersonResponse(
                    p.getName().value(), p.getEmail().value(),
                    p.getPhone().value(), p.getBirthDate());
            case final Company co -> new CompanyResponse(
                    co.getName().value(), co.getEmail().value(),
                    co.getPhone().value(), co.getCompanyIdentifier());
            default -> throw new IllegalStateException("Unknown client subtype");
        };
    }
}