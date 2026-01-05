package com.mk.contractservice.application.feature.client.create;

import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.factory.CompanyFactory;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientValidationService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public interface CreateCompany {

    record Command(
            String name,
            String email,
            String phoneNumber,
            String companyIdentifier
    ) {
        public Command {
            Objects.requireNonNull(name, "Name cannot be null");
            Objects.requireNonNull(email, "Email cannot be null");
            Objects.requireNonNull(phoneNumber, "Phone number cannot be null");
            Objects.requireNonNull(companyIdentifier, "Company identifier cannot be null");
        }
    }

    Company execute(Command command);

    @Service
    @Transactional
    @RequiredArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
    class Handler implements CreateCompany {

        ClientRepository clientRepository;
        ClientValidationService clientValidationService;

        @Override
        public Company execute(final Command command) {
            clientValidationService.ensureEmailIsUnique(command.email());
            clientValidationService.ensurePhoneIsUnique(command.phoneNumber());
            clientValidationService.ensureCompanyIdentifierIsUnique(command.companyIdentifier());

            final Company company = CompanyFactory.createFromCommand(
                    command.name(),
                    command.email(),
                    command.phoneNumber(),
                    command.companyIdentifier()
            );

            return clientRepository.save(company);
        }
    }
}

