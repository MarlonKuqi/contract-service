package com.mk.contractservice.domain.client.service;

import com.mk.contractservice.application.feature.client.create.CreateCompany;
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

    private CreateCompany createCompany;

    @BeforeEach
    void setUp() {
        createCompany = new CreateCompany.Handler(clientRepository, clientValidationService);
    }

    @Test
    @DisplayName("Should create and persist company with valid data")
    void shouldCreateAndPersistCompany() {
        // Given
        String name = "Acme Corp";
        String email = "contact@acme.com";
        String phoneNumber = "+41221234567";
        String companyIdentifier = "CHE-123.456.789";

        CreateCompany.Command command = new CreateCompany.Command(name, email, phoneNumber, companyIdentifier);

        UUID savedCompanyId = UUID.randomUUID();
        when(clientRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            return Company.reconstituteFromDatabase(
                    savedCompanyId,
                    company.getName(),
                    company.getEmail(),
                    company.getPhone(),
                    company.getCompanyIdentifier()
            );
        });

        // When
        Company result = createCompany.execute(command);

        // Then
        verify(clientValidationService).ensureEmailIsUnique(email);
        verify(clientValidationService).ensureCompanyIdentifierIsUnique(companyIdentifier);
        verify(clientRepository).save(any(Company.class));

        assertThat(result.getId()).isEqualTo(savedCompanyId);
        assertThat(result.getName().getValue()).isEqualTo(name);
        assertThat(result.getEmail().getValue()).isEqualTo(email);
        assertThat(result.getPhone().getValue()).isEqualTo(phoneNumber);
        assertThat(result.getCompanyIdentifier().getValue()).isEqualTo(companyIdentifier);
    }

    @Test
    @DisplayName("Should validate email uniqueness before creation")
    void shouldValidateEmailUniqueness() {
        // Given
        CreateCompany.Command command = new CreateCompany.Command(
                "Tech Inc",
                "info@tech.com",
                "+41221234568",
                "CHE-987.654.321"
        );

        doThrow(new ClientAlreadyExistsException("Client already exists", "info@tech.com"))
                .when(clientValidationService).ensureEmailIsUnique(any(String.class));

        // When / Then
        assertThatThrownBy(() -> createCompany.execute(command))
                .isInstanceOf(ClientAlreadyExistsException.class)
                .hasMessageContaining("Client already exists");

        verify(clientValidationService).ensureEmailIsUnique("info@tech.com");
        verify(clientValidationService, never()).ensureCompanyIdentifierIsUnique(any(String.class));
        verify(clientRepository, never()).save(any(Company.class));
    }

    @Test
    @DisplayName("Should validate company identifier uniqueness before creation")
    void shouldValidateCompanyIdentifierUniqueness() {
        // Given
        CreateCompany.Command command = new CreateCompany.Command(
                "Global Corp",
                "contact@global.com",
                "+41221234569",
                "CHE-111.222.333"
        );

        doThrow(new CompanyIdentifierAlreadyExistsException(
                "A company with identifier 'CHE-111.222.333' already exists",
                "CHE-111.222.333"))
                .when(clientValidationService).ensureCompanyIdentifierIsUnique(any(String.class));

        // When / Then
        assertThatThrownBy(() -> createCompany.execute(command))
                .isInstanceOf(CompanyIdentifierAlreadyExistsException.class)
                .hasMessageContaining("CHE-111.222.333");

        verify(clientValidationService).ensureEmailIsUnique(any(String.class));
        verify(clientValidationService).ensureCompanyIdentifierIsUnique("CHE-111.222.333");
        verify(clientRepository, never()).save(any(Company.class));
    }

    @Test
    @DisplayName("Should create company with all getValue objects properly validated")
    void shouldCreateCompanyWithValidatedValueObjects() {
        // Given
        CreateCompany.Command command = new CreateCompany.Command(
                "Swiss AG",
                "info@swiss.ch",
                "+41221234570",
                "CHE-555.666.777"
        );

        when(clientRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Company result = createCompany.execute(command);

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
        CreateCompany.Command command = new CreateCompany.Command(
                "Business Ltd",
                "contact@business.com",
                "+41221234571",
                "CHE-999.888.777"
        );

        when(clientRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        createCompany.execute(command);

        // Then
        verify(clientRepository).save(argThat(client ->
                client instanceof Company company &&
                        company.getName().getValue().equals("Business Ltd") &&
                        company.getCompanyIdentifier().getValue().equals("CHE-999.888.777")
        ));
    }
}

