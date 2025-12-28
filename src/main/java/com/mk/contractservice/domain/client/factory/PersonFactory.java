package com.mk.contractservice.domain.client.factory;

import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class PersonFactory {

    public static Person create(
            final String name,
            final String email,
            final String phoneNumber,
            final LocalDate birthDate
    ) {
        final ClientName clientName = ClientName.of(name);
        final ClientEmail clientEmail = ClientEmail.of(email);
        final ClientPhoneNumber phone = ClientPhoneNumber.of(phoneNumber);
        final PersonBirthDate personBirthDate = PersonBirthDate.of(birthDate);

        return Person.of(clientName, clientEmail, phone, personBirthDate);
    }
}

