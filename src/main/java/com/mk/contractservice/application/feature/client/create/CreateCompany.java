package com.mk.contractservice.application.feature.client.create;

import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientValidationService;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public sealed interface CreateCompany permits CreateCompany.Handler {

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
    non-sealed class Handler implements CreateCompany {

        ClientRepository clientRepository;
        ClientValidationService clientValidationService;

        @Override
        public Company execute(final Command command) {
            final ClientName name = ClientName.of(command.name());
            final ClientEmail email = ClientEmail.of(command.email());
            final ClientPhoneNumber phone = ClientPhoneNumber.of(command.phoneNumber());
            final CompanyIdentifier companyId = CompanyIdentifier.of(command.companyIdentifier());

            clientValidationService.ensureEmailIsUnique(email);
            clientValidationService.ensureCompanyIdentifierIsUnique(companyId);

            final Company company = Company.of(name, email, phone, companyId);
            return (Company) clientRepository.save(company);
        }
    }
}

