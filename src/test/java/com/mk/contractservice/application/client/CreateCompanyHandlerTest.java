package com.mk.contractservice.application.client;


import com.mk.contractservice.application.feature.client.create.CreateCompany;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.client.exception.EmailAlreadyExistsException;
import com.mk.contractservice.domain.client.exception.PhoneAlreadyExistsException;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCompany Handler")
class CreateCompanyHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    private ClientValidationService clientValidationService;
    private CreateCompany.Handler createCompanyHandler;

    @BeforeEach
    void setUp() {
        clientValidationService = new ClientValidationService(clientRepository);
        createCompanyHandler = new CreateCompany.Handler(clientRepository, clientValidationService);
    }

    @Nested
    @DisplayName("Création d'entreprise")
    class CreateCompanyTest {

        @BeforeEach
        void setUp() {
            when(clientRepository.save(any(Company.class))).thenAnswer(invocation -> {
                Company company = invocation.getArgument(0);
                return Company.builder()
                        .id(UUID.randomUUID())
                        .name(company.getName())
                        .email(company.getEmail())
                        .phone(company.getPhone())
                        .companyIdentifier(company.getCompanyIdentifier())
                        .build();
            });
        }

        @Test
        @DisplayName("GIVEN commande valide WHEN execute THEN crée l'entreprise avec tous les champs")
        void shouldCreateCompanyWithAllFields() {
            // Given
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech Corp",
                    "contact@techcorp.com",
                    "+33123456789",
                    "123456789"
            );

            // When
            Company result = createCompanyHandler.execute(command);

            // Then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getName().getValue()).isEqualTo(command.name());
            assertThat(result.getEmail().getValue()).isEqualTo(command.email());
            assertThat(result.getPhone().getValue()).isEqualTo(command.phoneNumber());
            assertThat(result.getCompanyIdentifier().getValue()).isEqualTo(command.companyIdentifier());
        }
    }

    @Nested
    @DisplayName("Erreurs de validation")
    class ValidationErrors {

        @Test
        @DisplayName("GIVEN email déjà existant WHEN execute THEN lève ClientAlreadyExistsException")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Given
            String duplicateEmail = "existing@techcorp.com";
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech Corp",
                    duplicateEmail,
                    "+33123456789",
                    "123456789"
            );

            when(clientRepository.existsByEmail(duplicateEmail)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> createCompanyHandler.execute(command))
                    .isInstanceOf(EmailAlreadyExistsException.class);
        }

        @Test
        @DisplayName("GIVEN identifiant déjà existant WHEN execute THEN lève CompanyIdentifierAlreadyExistsException")
        void shouldThrowExceptionWhenCompanyIdentifierAlreadyExists() {
            // Given
            String duplicateIdentifier = "123456789";
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech Corp",
                    "contact@techcorp.com",
                    "+33123456789",
                    duplicateIdentifier
            );

            when(clientRepository.existsByCompanyIdentifier(duplicateIdentifier)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> createCompanyHandler.execute(command))
                    .isInstanceOf(CompanyIdentifierAlreadyExistsException.class);
        }

        @Test
        @DisplayName("GIVEN phone number déjà existant WHEN execute THEN lève PhoneAlreadyExistsException")
        void shouldThrowExceptionPhoneNumberAlreadyExists() {
            // Given
            String duplicatePhone = "+33123456789";
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech Corp",
                    "contact@techcorp.com",
                    duplicatePhone,
                    "123456789"
            );

            when(clientRepository.existsByPhoneNumber(duplicatePhone)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> createCompanyHandler.execute(command))
                    .isInstanceOf(PhoneAlreadyExistsException.class);
        }
    }
}

