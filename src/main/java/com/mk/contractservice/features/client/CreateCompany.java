package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientValidationService;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.CompanyFactory;
import lombok.RequiredArgsConstructor;
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
    class Handler implements CreateCompany {

        private final ClientRepository clientRepository;
        private final ClientValidationService clientValidationService;

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

