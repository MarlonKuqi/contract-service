package com.mk.contractservice.infrastructure.web.client.shared;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
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

