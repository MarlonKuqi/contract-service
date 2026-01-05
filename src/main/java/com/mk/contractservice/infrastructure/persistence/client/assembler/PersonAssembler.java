package com.mk.contractservice.infrastructure.persistence.client.assembler;

import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.factory.PersonFactory;
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
        return PersonFactory.buildFromDatabase(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getBirthDate()
        );
    }
}

