package com.mk.contractservice.application.client;

import com.mk.contractservice.application.client.usecase.CreateCompanyUseCase;
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

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class CreateCompanyUseCaseImpl implements CreateCompanyUseCase {

    ClientRepository clientRepository;
    ClientValidationService clientValidationService;

    @Override
    public Company execute(CreateCompanyCommand command) {
        final ClientName name = ClientName.of(command.name());
        final ClientEmail email = ClientEmail.of(command.email());
        final ClientPhoneNumber phone = ClientPhoneNumber.of(command.phoneNumber());
        final CompanyIdentifier companyIdentifier = CompanyIdentifier.of(command.companyIdentifier());

        clientValidationService.ensureEmailIsUnique(email);
        clientValidationService.ensureCompanyIdentifierIsUnique(companyIdentifier);

        final Company company = Company.of(name, email, phone, companyIdentifier);
        return (Company) clientRepository.save(company);
    }
}

