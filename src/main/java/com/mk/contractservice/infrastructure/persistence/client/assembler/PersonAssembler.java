package com.mk.contractservice.infrastructure.persistence.client.assembler;

import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonBirthDate;
import com.mk.contractservice.infrastructure.persistence.client.entity.PersonJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PersonAssembler {


    public PersonJpaEntity toJpaEntity(final Person person) {
        final PersonJpaEntity entity = PersonJpaEntity.create(
                person.getName().value(),
                person.getEmail().value(),
                person.getPhone().value(),
                person.getBirthDate().value()
        );

        if (person.getId() != null) {
            entity.setId(person.getId());
        }

        return entity;
    }

    public Person toDomain(final PersonJpaEntity entity) {
        return Person.reconstitute(
                entity.getId(),
                ClientName.of(entity.getName()),
                ClientEmail.of(entity.getEmail()),
                ClientPhoneNumber.of(entity.getPhone()),
                PersonBirthDate.of(entity.getBirthDate())
        );
    }
}

