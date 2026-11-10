package com.mk.contractservice.domain.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientFixtures {

    public static Person newPerson(
            final String name,
            final String email,
            final String phoneNumber,
            final LocalDate birthDate,
            final LocalDate today
    ) {
        return Person.of(
                ClientName.of(name),
                ClientEmail.of(email),
                ClientPhoneNumber.of(phoneNumber),
                PersonBirthDate.of(birthDate, today)
        );
    }

    public static Person existingPerson(
            final UUID id,
            final String name,
            final String email,
            final String phoneNumber,
            final LocalDate birthDate
    ) {
        return Person.reconstituteFromDatabase(
                id,
                ClientName.reconstituteFromDatabase(name),
                ClientEmail.reconstituteFromDatabase(email),
                ClientPhoneNumber.reconstituteFromDatabase(phoneNumber),
                PersonBirthDate.reconstituteFromDatabase(birthDate)
        );
    }

    public static Person existingPerson(final UUID id) {
        return existingPerson(id,
                "Alice Martin",
                "alice.martin@example.com",
                "+41791234567",
                LocalDate.of(1990, 5, 14));
    }

    public static Company existingCompany(final UUID id) {
        return existingCompany(id, "Acme SA", "contact@acme.example.com", "+41223456789", "CHE-123.456.789");
    }

    public static Company newCompany(
            final String name,
            final String email,
            final String phoneNumber,
            final String companyIdentifier
    ) {
        return Company.of(
                ClientName.of(name),
                ClientEmail.of(email),
                ClientPhoneNumber.of(phoneNumber),
                CompanyIdentifier.of(companyIdentifier)
        );
    }

    public static Company existingCompany(
            final UUID id,
            final String name,
            final String email,
            final String phoneNumber,
            final String companyIdentifier
    ) {
        return Company.reconstituteFromDatabase(
                id,
                ClientName.reconstituteFromDatabase(name),
                ClientEmail.reconstituteFromDatabase(email),
                ClientPhoneNumber.reconstituteFromDatabase(phoneNumber),
                CompanyIdentifier.reconstituteFromDatabase(companyIdentifier)
        );
    }
}
