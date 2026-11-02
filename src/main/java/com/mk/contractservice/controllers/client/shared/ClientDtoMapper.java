package com.mk.contractservice.controllers.client.shared;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientDetails;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.CompanyDetails;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonDetails;
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

    default ClientResponse toResponse(final ClientDetails client) {
        return switch (client) {
            case PersonDetails p -> new PersonResponse(p.id(), p.name(), p.email(), p.phone(), p.birthDate());
            case CompanyDetails c -> new CompanyResponse(c.id(), c.name(), c.email(), c.phone(), c.companyIdentifier());
        };
    }
}

