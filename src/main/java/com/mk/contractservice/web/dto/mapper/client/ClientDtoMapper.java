package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.application.dto.ClientDto;
import com.mk.contractservice.application.dto.CompanyDto;
import com.mk.contractservice.application.dto.PersonDto;
import com.mk.contractservice.web.dto.client.ClientResponse;
import com.mk.contractservice.web.dto.client.CompanyResponse;
import com.mk.contractservice.web.dto.client.PersonResponse;
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