package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.PersonBirthDate;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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

    Client execute(Command command);

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements CreatePerson {

        private final ClientRepository clientRepository;
        private final Clock clock;

        @Override
        public Client execute(final Command command) {
            final Person person = Person.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    PersonBirthDate.of(command.birthDate(), LocalDate.now(clock))
            );
            return clientRepository.save(person);
        }
    }
}
