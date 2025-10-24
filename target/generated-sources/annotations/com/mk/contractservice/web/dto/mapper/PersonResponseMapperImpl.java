package com.mk.contractservice.web.dto.mapper;

import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.web.dto.client.PersonResponse;
import com.mk.contractservice.web.dto.mapper.client.PersonResponseMapper;
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
public class PersonResponseMapperImpl implements PersonResponseMapper {

    @Override
    public PersonResponse toDto(Person p) {
        if ( p == null ) {
            return null;
        }

        UUID id = null;
        LocalDate birthDate = null;

        id = p.getId();
        birthDate = p.getBirthDate();

        String name = p.getName().value();
        String email = p.getEmail().value();
        String phone = p.getPhone().value();

        PersonResponse personResponse = new PersonResponse( id, name, email, phone, birthDate );

        return personResponse;
    }
}
