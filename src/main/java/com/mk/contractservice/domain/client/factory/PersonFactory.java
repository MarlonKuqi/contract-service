package com.mk.contractservice.domain.client.factory;

import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class PersonFactory {

    public static Person createFromCommand(
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

    public static Person buildFromDatabase(
            final UUID id,
            final String name,
            final String email,
            final String phoneNumber,
            final LocalDate birthDate
    ) {
        final ClientName clientName = ClientName.reconstituteFromDatabase(name);
        final ClientEmail clientEmail = ClientEmail.reconstituteFromDatabase(email);
        final ClientPhoneNumber phone = ClientPhoneNumber.reconstituteFromDatabase(phoneNumber);
        final PersonBirthDate personBirthDate = PersonBirthDate.reconstituteFromDatabase(birthDate);

        return Person.reconstituteFromDatabase(id, clientName, clientEmail, phone, personBirthDate);
    }
}

