package com.mk.contractservice.application.mapper;

import com.mk.contractservice.application.dto.ClientDto;
import com.mk.contractservice.application.dto.CompanyDto;
import com.mk.contractservice.application.dto.PersonDto;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ValueObjectMappers.class)
public interface ClientMapper {

    default ClientDto toDto(final Client client) {
        return switch (client) {
            case Person person -> toPersonDto(person);
            case Company company -> toCompanyDto(company);
        };
    }

    PersonDto toPersonDto(Person person);

    CompanyDto toCompanyDto(Company company);
}

