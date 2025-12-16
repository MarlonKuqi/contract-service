package com.mk.contractservice.application.client.mapper;

import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
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

