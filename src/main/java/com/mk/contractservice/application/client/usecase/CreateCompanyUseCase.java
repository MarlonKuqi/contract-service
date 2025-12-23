package com.mk.contractservice.application.client.usecase;

import com.mk.contractservice.domain.client.aggregate.Company;

import java.util.Objects;

public interface CreateCompanyUseCase {

    Company execute(CreateCompanyCommand command);

    record CreateCompanyCommand(
            String name,
            String email,
            String phoneNumber,
            String companyIdentifier
    ) {
        public CreateCompanyCommand {
            Objects.requireNonNull(name, "Name cannot be null");
            Objects.requireNonNull(email, "Email cannot be null");
            Objects.requireNonNull(phoneNumber, "Phone number cannot be null");
            Objects.requireNonNull(companyIdentifier, "Company identifier cannot be null");
        }
    }
}

