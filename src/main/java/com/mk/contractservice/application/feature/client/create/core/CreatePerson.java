package com.mk.contractservice.application.feature.client.create.core;

import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientValidationService;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;


public sealed interface CreatePerson permits CreatePerson.Handler {

    record Command(
            String name,
            String email,
            String phoneNumber,
            LocalDate birthDate
    ) {
        public Command {
            Objects.requireNonNull(name, "Name cannot be null");
            Objects.requireNonNull(email, "Email cannot be null");
            Objects.requireNonNull(phoneNumber, "Phone number cannot be null");
            Objects.requireNonNull(birthDate, "Birth date cannot be null");
        }
    }

    Person execute(Command command);

    @Service
    @Transactional
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    non-sealed class Handler implements CreatePerson {

        ClientRepository clientRepository;
        ClientValidationService clientValidationService;

        @Override
        public Person execute(final Command command) {
            final ClientName name = ClientName.of(command.name());
            final ClientEmail email = ClientEmail.of(command.email());
            final ClientPhoneNumber phone = ClientPhoneNumber.of(command.phoneNumber());
            final PersonBirthDate birthDate = PersonBirthDate.of(command.birthDate());

            clientValidationService.ensureEmailIsUnique(email);

            final Person person = Person.of(name, email, phone, birthDate);
            return (Person) clientRepository.save(person);
        }
    }
}

