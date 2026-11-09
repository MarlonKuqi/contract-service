package com.mk.contractservice.infrastructure.persistence.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.CompanyIdentifier;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonBirthDate;
import com.mk.contractservice.infrastructure.persistence.client.entities.ClientJpaEntity;
import com.mk.contractservice.infrastructure.persistence.client.entities.CompanyJpaEntity;
import com.mk.contractservice.infrastructure.persistence.client.entities.PersonJpaEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientJpaMapper {

    public static Client toDomain(final ClientJpaEntity entity) {
        return switch (entity) {
            case PersonJpaEntity p -> Person.reconstituteFromDatabase(
                    p.getId(),
                    ClientName.reconstituteFromDatabase(p.getName()),
                    ClientEmail.reconstituteFromDatabase(p.getEmail()),
                    ClientPhoneNumber.reconstituteFromDatabase(p.getPhone()),
                    PersonBirthDate.reconstituteFromDatabase(p.getBirthDate())
            );
            case CompanyJpaEntity c -> Company.reconstituteFromDatabase(
                    c.getId(),
                    ClientName.reconstituteFromDatabase(c.getName()),
                    ClientEmail.reconstituteFromDatabase(c.getEmail()),
                    ClientPhoneNumber.reconstituteFromDatabase(c.getPhone()),
                    CompanyIdentifier.reconstituteFromDatabase(c.getCompanyIdentifier())
            );
            default -> throw new IllegalArgumentException("Unknown client entity type: " + entity.getClass().getName());
        };
    }

    public static PersonJpaEntity toNewPersonEntity(final Person person) {
        return new PersonJpaEntity(
                person.name().value(),
                person.email().value(),
                person.phone().value(),
                person.birthDate().value()
        );
    }

    public static CompanyJpaEntity toNewCompanyEntity(final Company company) {
        return new CompanyJpaEntity(
                company.name().value(),
                company.email().value(),
                company.phone().value(),
                company.companyIdentifier().value()
        );
    }

    public static void mergeIntoExisting(final Client domain, final ClientJpaEntity existing) {
        existing.setName(domain.name().value());
        existing.setEmail(domain.email().value());
        existing.setPhone(domain.phone().value());
    }
}
