package com.mk.contractservice.controllers.client.shared;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientDetails;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.CompanyDetails;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonDetails;
import org.mapstruct.Mapper;

import java.util.Objects;

@Mapper(componentModel = "spring")
public interface ClientDtoMapper {

    default ClientResponse toResponse(final Client client) {
        return switch (client) {
            case Person p -> new PersonResponse(
                    Objects.requireNonNull(p.id()),
                    p.name().value(),
                    p.email().value(),
                    p.phone().value(),
                    p.birthDate().value()
            );
            case Company c -> new CompanyResponse(
                    Objects.requireNonNull(c.id()),
                    c.name().value(),
                    c.email().value(),
                    c.phone().value(),
                    c.companyIdentifier().value()
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
