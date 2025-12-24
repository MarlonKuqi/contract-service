package com.mk.contractservice.web.client.mapper;

import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.web.client.dto.ClientResponse;
import com.mk.contractservice.web.client.dto.CompanyResponse;
import com.mk.contractservice.web.client.dto.PersonResponse;
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
