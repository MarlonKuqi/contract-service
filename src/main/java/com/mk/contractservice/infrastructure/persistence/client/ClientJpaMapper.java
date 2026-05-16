package com.mk.contractservice.infrastructure.persistence.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.CompanyFactory;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonFactory;
import com.mk.contractservice.infrastructure.persistence.client.entities.ClientJpaEntity;
import com.mk.contractservice.infrastructure.persistence.client.entities.CompanyJpaEntity;
import com.mk.contractservice.infrastructure.persistence.client.entities.PersonJpaEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientJpaMapper {

    public static Client toDomain(final ClientJpaEntity entity) {
        return switch (entity) {
            case PersonJpaEntity p -> PersonFactory.buildFromDatabase(
                    p.getId(), p.getName(), p.getEmail(), p.getPhone(), p.getBirthDate()
            );
            case CompanyJpaEntity c -> CompanyFactory.buildFromDatabase(
                    c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getCompanyIdentifier()
            );
            default -> throw new IllegalArgumentException("Unknown client entity type: " + entity.getClass().getName());
        };
    }

    public static PersonJpaEntity toNewPersonEntity(final Person person) {
        return PersonJpaEntity.create(
                person.getName().getValue(),
                person.getEmail().getValue(),
                person.getPhone().getValue(),
                person.getBirthDate().getValue()
        );
    }

    public static CompanyJpaEntity toNewCompanyEntity(final Company company) {
        return CompanyJpaEntity.create(
                company.getName().getValue(),
                company.getEmail().getValue(),
                company.getPhone().getValue(),
                company.getCompanyIdentifier().getValue()
        );
    }

    public static void mergeIntoExisting(final Client domain, final ClientJpaEntity existing) {
        existing.setName(domain.getName().getValue());
        existing.setEmail(domain.getEmail().getValue());
        existing.setPhone(domain.getPhone().getValue());
    }
}
