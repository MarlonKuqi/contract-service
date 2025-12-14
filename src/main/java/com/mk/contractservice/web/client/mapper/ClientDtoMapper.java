package com.mk.contractservice.web.client.mapper;

import com.mk.contractservice.application.client.dto.ClientDto;
import com.mk.contractservice.application.client.dto.CompanyDto;
import com.mk.contractservice.application.client.dto.PersonDto;
import com.mk.contractservice.web.client.dto.ClientResponse;
import com.mk.contractservice.web.client.dto.CompanyResponse;
import com.mk.contractservice.web.client.dto.PersonResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientDtoMapper {

    default ClientResponse toResponse(final ClientDto clientDto) {
        return switch (clientDto) {
            case PersonDto p -> new PersonResponse(
                    p.id(),
                    p.name(),
                    p.email(),
                    p.phone(),
                    p.birthDate()
            );
            case CompanyDto c -> new CompanyResponse(
                    c.id(),
                    c.name(),
                    c.email(),
                    c.phone(),
                    c.companyIdentifier()
            );
        };
    }
}