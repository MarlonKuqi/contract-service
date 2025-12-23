package com.mk.contractservice.domain.client.service;

import com.mk.contractservice.application.client.CreateCompanyUseCaseImpl;
import com.mk.contractservice.application.client.usecase.CreateCompanyUseCase;
import com.mk.contractservice.application.client.usecase.CreateCompanyUseCase.CreateCompanyCommand;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.client.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCompanyService - Use Case Tests")
class CreateCompanyServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientValidationService clientValidationService;

    private CreateCompanyUseCase service;

    @BeforeEach
    void setUp() {
        service = new CreateCompanyUseCaseImpl(clientRepository, clientValidationService);
    }

    @Test
    @DisplayName("Should create and persist company with valid data")
    void shouldCreateAndPersistCompany() {
        // Given
        String name = "Acme Corp";
        String email = "contact@acme.com";
        String phoneNumber = "+41221234567";
        String companyIdentifier = "CHE-123.456.789";

        CreateCompanyCommand command = new CreateCompanyCommand(name, email, phoneNumber, companyIdentifier);

        UUID savedCompanyId = UUID.randomUUID();
        when(clientRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            return Company.reconstitute(
                    savedCompanyId,
                    company.getName(),
                    company.getEmail(),
                    company.getPhone(),
                    company.getCompanyIdentifier()
            );
        });

        // When
        Company result = service.execute(command);

        // Then
        verify(clientValidationService).ensureEmailIsUnique(ClientEmail.of(email));
        verify(clientValidationService).ensureCompanyIdentifierIsUnique(CompanyIdentifier.of(companyIdentifier));
        verify(clientRepository).save(any(Company.class));

        assertThat(result.getId()).isEqualTo(savedCompanyId);
        assertThat(result.getName().value()).isEqualTo(name);
        assertThat(result.getEmail().value()).isEqualTo(email);
        assertThat(result.getPhone().value()).isEqualTo(phoneNumber);
        assertThat(result.getCompanyIdentifier().value()).isEqualTo(companyIdentifier);
    }

    @Test
    @DisplayName("Should validate email uniqueness before creation")
    void shouldValidateEmailUniqueness() {
        // Given
        CreateCompanyCommand command = new CreateCompanyCommand(
                "Tech Inc",
                "info@tech.com",
                "+41221234568",
                "CHE-987.654.321"
        );

        doThrow(new ClientAlreadyExistsException("Client already exists", "info@tech.com"))
                .when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));

        // When / Then
        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(ClientAlreadyExistsException.class)
                .hasMessageContaining("Client already exists");

        verify(clientValidationService).ensureEmailIsUnique(ClientEmail.of("info@tech.com"));
        verify(clientValidationService, never()).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
        verify(clientRepository, never()).save(any(Company.class));
    }

    @Test
    @DisplayName("Should validate company identifier uniqueness before creation")
    void shouldValidateCompanyIdentifierUniqueness() {
        // Given
        CreateCompanyCommand command = new CreateCompanyCommand(
                "Global Corp",
                "contact@global.com",
                "+41221234569",
                "CHE-111.222.333"
        );

        doThrow(new CompanyIdentifierAlreadyExistsException(
                "A company with identifier 'CHE-111.222.333' already exists",
                "CHE-111.222.333"))
                .when(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));

        // When / Then
        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(CompanyIdentifierAlreadyExistsException.class)
                .hasMessageContaining("CHE-111.222.333");

        verify(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
        verify(clientValidationService).ensureCompanyIdentifierIsUnique(CompanyIdentifier.of("CHE-111.222.333"));
        verify(clientRepository, never()).save(any(Company.class));
    }

    @Test
    @DisplayName("Should create company with all value objects properly validated")
    void shouldCreateCompanyWithValidatedValueObjects() {
        // Given
        CreateCompanyCommand command = new CreateCompanyCommand(
                "Swiss AG",
                "info@swiss.ch",
                "+41221234570",
                "CHE-555.666.777"
        );

        when(clientRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Company result = service.execute(command);

        // Then
        assertThat(result.getName()).isEqualTo(ClientName.of("Swiss AG"));
        assertThat(result.getEmail()).isEqualTo(ClientEmail.of("info@swiss.ch"));
        assertThat(result.getPhone()).isEqualTo(ClientPhoneNumber.of("+41221234570"));
        assertThat(result.getCompanyIdentifier()).isEqualTo(CompanyIdentifier.of("CHE-555.666.777"));
    }

    @Test
    @DisplayName("Should delegate persistence to repository")
    void shouldDelegatePersistenceToRepository() {
        // Given
        CreateCompanyCommand command = new CreateCompanyCommand(
                "Business Ltd",
                "contact@business.com",
                "+41221234571",
                "CHE-999.888.777"
        );

        when(clientRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        service.execute(command);

        // Then
        verify(clientRepository).save(argThat(client ->
                client instanceof Company company &&
                        company.getName().value().equals("Business Ltd") &&
                        company.getCompanyIdentifier().value().equals("CHE-999.888.777")
        ));
    }
}

