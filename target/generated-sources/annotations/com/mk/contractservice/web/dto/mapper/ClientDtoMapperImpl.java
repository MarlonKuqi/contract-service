package com.mk.contractservice.web.dto.mapper;

import com.mk.contractservice.domain.client.ClientType;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.web.dto.mapper.client.ClientDtoMapper;
import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-20T01:10:43+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class ClientDtoMapperImpl implements ClientDtoMapper {

    @Override
    public ClientResponse toResponse(Person p) {
        if ( p == null ) {
            return null;
        }

        LocalDate birthDate = null;
        UUID id = null;

        birthDate = p.getBirthDate();
        id = p.getId();

        ClientType type = ClientType.PERSON;
        String name = p.getName().value();
        String email = p.getEmail().value();
        String phone = p.getPhone().value();
        String companyIdentifier = null;

        ClientResponse clientResponse = new ClientResponse( id, type, name, email, phone, birthDate, companyIdentifier );

        return clientResponse;
    }

    @Override
    public ClientResponse toResponse(Company c) {
        if ( c == null ) {
            return null;
        }

        String companyIdentifier = null;
        UUID id = null;

        companyIdentifier = c.getCompanyIdentifier();
        id = c.getId();

        ClientType type = ClientType.COMPANY;
        String name = c.getName().value();
        String email = c.getEmail().value();
        String phone = c.getPhone().value();
        LocalDate birthDate = null;

        ClientResponse clientResponse = new ClientResponse( id, type, name, email, phone, birthDate, companyIdentifier );

        return clientResponse;
    }
}
