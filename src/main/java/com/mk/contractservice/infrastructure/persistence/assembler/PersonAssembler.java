package com.mk.contractservice.infrastructure.persistence.assembler;

import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.infrastructure.persistence.entity.PersonJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PersonAssembler {

    public PersonJpaEntity toJpaEntity(final Person person) {
        if (person == null) {
            return null;
        }

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
        if (entity == null) {
            return null;
        }

        return Person.reconstitute(
                entity.getId(),
                ClientName.of(entity.getName()),
                Email.of(entity.getEmail()),
                PhoneNumber.of(entity.getPhone()),
                PersonBirthDate.of(entity.getBirthDate())
        );
    }
}

