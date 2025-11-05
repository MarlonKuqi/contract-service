package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.web.dto.client.ClientResponse;
import com.mk.contractservice.web.dto.client.CompanyResponse;
import com.mk.contractservice.web.dto.client.PersonResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientDtoMapper {

    default ClientResponse toResponse(final Client client) {
        return switch (client) {
            case final Person p -> new PersonResponse(
                    p.getId(),
                    p.getName(),
                    p.getEmail(),
                    p.getPhone(),
                    p.getBirthDate()
            );
            case final Company co -> new CompanyResponse(
                    co.getId(),
                    co.getName(),
                    co.getEmail(),
                    co.getPhone(),
                    co.getCompanyIdentifier()
            );
            default -> throw new IllegalStateException("Unknown client subtype");
        };
    }
}