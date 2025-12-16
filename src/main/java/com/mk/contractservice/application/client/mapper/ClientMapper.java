package com.mk.contractservice.application.client.mapper;

import com.mk.contractservice.application.client.dto.ClientDto;
import com.mk.contractservice.application.client.dto.CompanyDto;
import com.mk.contractservice.application.client.dto.PersonDto;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.aggregate.Person;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ClientValueObjectMappers.class)
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

