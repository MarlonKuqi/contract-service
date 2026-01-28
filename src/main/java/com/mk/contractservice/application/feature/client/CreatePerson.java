package com.mk.contractservice.application.feature.client;

import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonFactory;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientValidationService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;


public interface CreatePerson {

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
    class Handler implements CreatePerson {

        ClientRepository clientRepository;
        ClientValidationService clientValidationService;

        @Override
        public Person execute(final Command command) {
            clientValidationService.ensureEmailIsUnique(command.email());
            clientValidationService.ensurePhoneIsUnique(command.phoneNumber());

            final Person person = PersonFactory.createFromCommand(
                    command.name(),
                    command.email(),
                    command.phoneNumber(),
                    command.birthDate()
            );

            return clientRepository.save(person);
        }
    }
}

