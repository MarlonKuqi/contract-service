package com.mk.contractservice.application.client.usecase;

import com.mk.contractservice.domain.client.aggregate.Person;

import java.time.LocalDate;
import java.util.Objects;


public interface CreatePersonUseCase {

    Person execute(CreatePersonCommand command);

    record CreatePersonCommand(
            String name,
            String email,
            String phoneNumber,
            LocalDate birthDate
    ) {
        public CreatePersonCommand {
            Objects.requireNonNull(name, "Name cannot be null");
            Objects.requireNonNull(email, "Email cannot be null");
            Objects.requireNonNull(phoneNumber, "Phone number cannot be null");
            Objects.requireNonNull(birthDate, "Birth date cannot be null");
        }
    }
}

