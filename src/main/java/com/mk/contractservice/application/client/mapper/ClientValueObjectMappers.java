package com.mk.contractservice.application.client.mapper;

import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.CompanyIdentifier;
import com.mk.contractservice.domain.client.PersonBirthDate;
import org.mapstruct.Mapper;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface ClientValueObjectMappers {

    default String clientNameToString(ClientName name) {
        return name.value();
    }

    default String emailToString(ClientEmail email) {
        return email.value();
    }

    default String phoneNumberToString(ClientPhoneNumber phone) {
        return phone.value();
    }

    default LocalDate personBirthDateToLocalDate(PersonBirthDate date) {
        return date.value();
    }

    default String companyIdentifierToString(CompanyIdentifier identifier) {
        return identifier.value();
    }
}

