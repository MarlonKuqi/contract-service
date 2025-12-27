package com.mk.contractservice.application.feature.client.shared.mapper;

import com.mk.contractservice.application.feature.client.shared.response.ClientResponse;
import com.mk.contractservice.application.feature.client.shared.response.CompanyResponse;
import com.mk.contractservice.application.feature.client.shared.response.PersonResponse;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.aggregate.Person;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientDtoMapper {

    default ClientResponse toResponse(final Client client) {
        return switch (client) {
            case Person p -> new PersonResponse(
                    p.getId(),
                    p.getName().getValue(),
                    p.getEmail().getValue(),
                    p.getPhone().getValue(),
                    p.getBirthDate().getValue()
            );
            case Company c -> new CompanyResponse(
                    c.getId(),
                    c.getName().getValue(),
                    c.getEmail().getValue(),
                    c.getPhone().getValue(),
                    c.getCompanyIdentifier().getValue()
            );
        };
    }
}

