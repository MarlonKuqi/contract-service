package com.mk.contractservice.application.mapper;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import org.mapstruct.Mapper;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface ValueObjectMappers {

    default String clientNameToString(ClientName name) {
        return name.value();
    }

    default String emailToString(Email email) {
        return email.value();
    }

    default String phoneNumberToString(PhoneNumber phone) {
        return phone.value();
    }

    default LocalDate personBirthDateToLocalDate(PersonBirthDate date) {
        return date.value();
    }

    default String companyIdentifierToString(CompanyIdentifier identifier) {
        return identifier.value();
    }
}

