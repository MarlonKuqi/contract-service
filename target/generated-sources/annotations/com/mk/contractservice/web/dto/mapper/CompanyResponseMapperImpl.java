package com.mk.contractservice.web.dto.mapper;

import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.web.dto.client.CompanyResponse;
import com.mk.contractservice.web.dto.mapper.client.CompanyResponseMapper;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-20T01:10:43+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class CompanyResponseMapperImpl implements CompanyResponseMapper {

    @Override
    public CompanyResponse toDto(Company c) {
        if ( c == null ) {
            return null;
        }

        UUID id = null;
        String companyIdentifier = null;

        id = c.getId();
        companyIdentifier = c.getCompanyIdentifier();

        String name = c.getName().value();
        String email = c.getEmail().value();
        String phone = c.getPhone().value();

        CompanyResponse companyResponse = new CompanyResponse( id, name, email, phone, companyIdentifier );

        return companyResponse;
    }
}
