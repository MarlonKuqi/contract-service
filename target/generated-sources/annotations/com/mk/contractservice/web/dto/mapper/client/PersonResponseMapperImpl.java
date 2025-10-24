package com.mk.contractservice.web.dto.mapper.client;

import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.web.dto.client.CreatePersonResponse;
import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-24T17:57:34+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class PersonResponseMapperImpl implements PersonResponseMapper {

    @Override
    public CreatePersonResponse toDto(Person p) {
        if ( p == null ) {
            return null;
        }

        Long id = null;
        LocalDate birthDate = null;

        id = p.getId();
        birthDate = p.getBirthDate();

        String name = p.getName().value();
        String email = p.getEmail().value();
        String phone = p.getPhone().value();

        CreatePersonResponse createPersonResponse = new CreatePersonResponse( id, name, email, phone, birthDate );

        return createPersonResponse;
    }
}
