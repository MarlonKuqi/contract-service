package com.mk.contractservice.web.dto.mapper;

import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.web.dto.client.CreateCompanyRequest;
import com.mk.contractservice.web.dto.client.CreatePersonRequest;
import com.mk.contractservice.web.dto.mapper.client.ClientCreatetMapper;
import com.mk.contractservice.web.dto.mapper.common.ValueObjectMappers;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-20T01:10:43+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class ClientCreatetMapperImpl implements ClientCreatetMapper {

    @Autowired
    private ValueObjectMappers valueObjectMappers;

    @Override
    public Person toEntity(CreatePersonRequest req) {
        if ( req == null ) {
            return null;
        }

        Person.PersonBuilder<?, ?> person = Person.builder();

        person.name( valueObjectMappers.toName( req.name() ) );
        person.email( valueObjectMappers.toEmail( req.email() ) );
        person.phone( valueObjectMappers.toPhone( req.phone() ) );
        person.birthDate( req.birthDate() );

        return person.build();
    }

    @Override
    public Company toEntity(CreateCompanyRequest req) {
        if ( req == null ) {
            return null;
        }

        Company.CompanyBuilder<?, ?> company = Company.builder();

        company.name( valueObjectMappers.toName( req.name() ) );
        company.email( valueObjectMappers.toEmail( req.email() ) );
        company.phone( valueObjectMappers.toPhone( req.phone() ) );
        company.companyIdentifier( req.companyIdentifier() );

        return company.build();
    }
}
