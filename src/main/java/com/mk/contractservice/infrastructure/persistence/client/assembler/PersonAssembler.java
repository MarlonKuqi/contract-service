package com.mk.contractservice.infrastructure.persistence.client.assembler;

import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import com.mk.contractservice.infrastructure.persistence.client.entity.PersonJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PersonAssembler {

    public PersonJpaEntity toJpaEntity(final Person person) {
        final PersonJpaEntity entity = PersonJpaEntity.create(
                person.getName().getValue(),
                person.getEmail().getValue(),
                person.getPhone().getValue(),
                person.getBirthDate().getValue()
        );

        if (person.getId() != null) {
            entity.setId(person.getId());
        }

        return entity;
    }

    public Person toDomain(final PersonJpaEntity entity) {
        return Person.reconstituteFromDatabase(
                entity.getId(),
                ClientName.reconstituteFromDatabase(entity.getName()),
                ClientEmail.reconstituteFromDatabase(entity.getEmail()),
                ClientPhoneNumber.reconstituteFromDatabase(entity.getPhone()),
                PersonBirthDate.reconstituteFromDatabase(entity.getBirthDate())
        );
    }
}

