package com.mk.contractservice.application.client;


import com.mk.contractservice.application.feature.client.create.core.CreateCompany;
import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientValidationService;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCompanyUseCase - Unit Tests")
class CreateCompanyHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientValidationService clientValidationService;

    @InjectMocks
    private CreateCompany.Handler createCompany;

    @Nested
    @DisplayName("execute() - Happy Path")
    class ExecuteHappyPath {

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should create and save company")
        void shouldCreateAndSaveCompany() {
            // Given
            String name = "Tech Corp";
            String email = "contact@techcorp.com";
            String phone = "+33123456789";
            String companyIdentifier = "123456789";

            CreateCompany.Command command = new CreateCompany.Command(
                    name,
                    email,
                    phone,
                    companyIdentifier
            );

            Company expectedCompany = Company.of(
                    ClientName.of(name),
                    ClientEmail.of(email),
                    ClientPhoneNumber.of(phone),
                    CompanyIdentifier.of(companyIdentifier)
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            doNothing().when(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
            when(clientRepository.save(any(Company.class))).thenReturn(expectedCompany);

            // When
            Company result = createCompany.execute(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName().getValue()).isEqualTo(name);
            assertThat(result.getEmail().getValue()).isEqualTo(email.toLowerCase());
            assertThat(result.getPhone().getValue()).isEqualTo(phone);
            assertThat(result.getCompanyIdentifier().getValue()).isEqualTo(companyIdentifier);

            verify(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            verify(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
            verify(clientRepository).save(any(Company.class));
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should validate email uniqueness")
        void shouldValidateEmailUniqueness() {
            // Given
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech Corp",
                    "contact@techcorp.com",
                    "+33123456789",
                    "123456789"
            );

            Company company = Company.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    CompanyIdentifier.of(command.companyIdentifier())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            doNothing().when(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
            when(clientRepository.save(any(Company.class))).thenReturn(company);

            // When
            createCompany.execute(command);

            // Then
            ArgumentCaptor<ClientEmail> emailCaptor = ArgumentCaptor.forClass(ClientEmail.class);
            verify(clientValidationService).ensureEmailIsUnique(emailCaptor.capture());
            assertThat(emailCaptor.getValue().getValue()).isEqualTo(command.email().toLowerCase());
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should validate company identifier uniqueness")
        void shouldValidateCompanyIdentifierUniqueness() {
            // Given
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech Corp",
                    "contact@techcorp.com",
                    "+33123456789",
                    "987654321"
            );

            Company company = Company.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    CompanyIdentifier.of(command.companyIdentifier())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            doNothing().when(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
            when(clientRepository.save(any(Company.class))).thenReturn(company);

            // When
            createCompany.execute(command);

            // Then
            ArgumentCaptor<CompanyIdentifier> identifierCaptor = ArgumentCaptor.forClass(CompanyIdentifier.class);
            verify(clientValidationService).ensureCompanyIdentifierIsUnique(identifierCaptor.capture());
            assertThat(identifierCaptor.getValue().getValue()).isEqualTo(command.companyIdentifier());
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should validate before saving")
        void shouldValidateBeforeSaving() {
            // Given
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech Corp",
                    "contact@techcorp.com",
                    "+33123456789",
                    "123456789"
            );

            Company company = Company.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    CompanyIdentifier.of(command.companyIdentifier())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            doNothing().when(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
            when(clientRepository.save(any(Company.class))).thenReturn(company);

            // When
            createCompany.execute(command);

            // Then
            var ordered = inOrder(clientValidationService, clientRepository);
            ordered.verify(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            ordered.verify(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
            ordered.verify(clientRepository).save(any(Company.class));
        }
    }

    @Nested
    @DisplayName("execute() - Validation Errors")
    class ExecuteValidationErrors {

        @Test
        @DisplayName("GIVEN duplicate email WHEN execute THEN should throw ClientAlreadyExistsException")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Given
            String duplicateEmail = "existing@techcorp.com";
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech Corp",
                    duplicateEmail,
                    "+33123456789",
                    "123456789"
            );

            doThrow(new ClientAlreadyExistsException("Email already exists: " + duplicateEmail))
                    .when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));

            // When & Then
            assertThatThrownBy(() -> createCompany.execute(command))
                    .isInstanceOf(ClientAlreadyExistsException.class)
                    .hasMessageContaining(duplicateEmail);

            verify(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            verify(clientValidationService, never()).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
            verify(clientRepository, never()).save(any(Company.class));
        }

        @Test
        @DisplayName("GIVEN duplicate company identifier WHEN execute THEN should throw ClientAlreadyExistsException")
        void shouldThrowExceptionWhenCompanyIdentifierAlreadyExists() {
            // Given
            String duplicateIdentifier = "123456789";
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech Corp",
                    "contact@techcorp.com",
                    "+33123456789",
                    duplicateIdentifier
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            doThrow(new ClientAlreadyExistsException("Company identifier already exists: " + duplicateIdentifier))
                    .when(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));

            // When & Then
            assertThatThrownBy(() -> createCompany.execute(command))
                    .isInstanceOf(ClientAlreadyExistsException.class)
                    .hasMessageContaining(duplicateIdentifier);

            verify(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            verify(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
            verify(clientRepository, never()).save(any(Company.class));
        }
    }

    @Nested
    @DisplayName("execute() - Edge Cases")
    class ExecuteEdgeCases {

        @Test
        @DisplayName("GIVEN email with uppercase WHEN execute THEN should normalize to lowercase")
        void shouldNormalizeEmailToLowercase() {
            // Given
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech Corp",
                    "Contact@TechCorp.COM",
                    "+33123456789",
                    "123456789"
            );

            Company company = Company.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    CompanyIdentifier.of(command.companyIdentifier())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            doNothing().when(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
            when(clientRepository.save(any(Company.class))).thenReturn(company);

            // When
            Company result = createCompany.execute(command);

            // Then
            assertThat(result.getEmail().getValue()).isEqualTo("contact@techcorp.com");
        }

        @Test
        @DisplayName("GIVEN company name with special characters WHEN execute THEN should create company")
        void shouldCreateCompanyWithSpecialCharactersInName() {
            // Given
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech & Innovation Corp.",
                    "contact@techcorp.com",
                    "+33123456789",
                    "123456789"
            );

            Company company = Company.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    CompanyIdentifier.of(command.companyIdentifier())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            doNothing().when(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
            when(clientRepository.save(any(Company.class))).thenReturn(company);

            // When
            Company result = createCompany.execute(command);

            // Then
            assertThat(result.getName().getValue()).isEqualTo("Tech & Innovation Corp.");
        }

        @Test
        @DisplayName("GIVEN international phone number WHEN execute THEN should create company")
        void shouldCreateCompanyWithInternationalPhone() {
            // Given
            CreateCompany.Command command = new CreateCompany.Command(
                    "Tech Corp",
                    "contact@techcorp.com",
                    "+441234567890",
                    "123456789"
            );

            Company company = Company.of(
                    ClientName.of(command.name()),
                    ClientEmail.of(command.email()),
                    ClientPhoneNumber.of(command.phoneNumber()),
                    CompanyIdentifier.of(command.companyIdentifier())
            );

            doNothing().when(clientValidationService).ensureEmailIsUnique(any(ClientEmail.class));
            doNothing().when(clientValidationService).ensureCompanyIdentifierIsUnique(any(CompanyIdentifier.class));
            when(clientRepository.save(any(Company.class))).thenReturn(company);

            // When
            Company result = createCompany.execute(command);

            // Then
            assertThat(result.getPhone().getValue()).isEqualTo("+441234567890");
        }
    }
}

