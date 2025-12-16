package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.client.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientService;
import com.mk.contractservice.domain.client.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService - Business Logic Tests")
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    private ClientService service;

    @BeforeEach
    void setUp() {
        service = new ClientService(clientRepository);
    }

    @Nested
    @DisplayName("Create Person - Business Rules")
    class CreatePersonTests {

        @Test
        @DisplayName("Should create Person when email is unique")
        void shouldCreatePersonWhenEmailIsUnique() {
            ClientName name = ClientName.of("John Doe");
            ClientEmail clientEmail = ClientEmail.of("john@example.com");
            ClientPhoneNumber phone = ClientPhoneNumber.of("+33123456789");
            PersonBirthDate birthDate = PersonBirthDate.of(LocalDate.of(1990, 1, 15));

            when(clientRepository.existsByEmail(clientEmail)).thenReturn(false);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
            Person result = service.createAndPersistPerson(name, clientEmail, phone, birthDate);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getEmail()).isEqualTo(clientEmail);
            assertThat(result.getPhone()).isEqualTo(phone);
            assertThat(result.getBirthDate()).isEqualTo(birthDate);

            verify(clientRepository).existsByEmail(clientEmail);
        }

        @Test
        @DisplayName("Should throw ClientAlreadyExistsException when email already exists")
        void shouldThrowWhenEmailAlreadyExists() {
            ClientName name = ClientName.of("John Doe");
            ClientEmail clientEmail = ClientEmail.of("duplicate@example.com");
            ClientPhoneNumber phone = ClientPhoneNumber.of("+33123456789");
            PersonBirthDate birthDate = PersonBirthDate.of(LocalDate.of(1990, 1, 15));

            when(clientRepository.existsByEmail(clientEmail)).thenReturn(true);

            assertThatThrownBy(() -> service.createAndPersistPerson(name, clientEmail, phone, birthDate))
                    .isInstanceOf(ClientAlreadyExistsException.class)
                    .hasMessageContaining("Client already exists")
                    .extracting("businessKey")
                    .isEqualTo(clientEmail.value());

            verify(clientRepository).existsByEmail(clientEmail);
        }

        @Test
        @DisplayName("Should create Person with all fields correctly mapped")
        void shouldMapAllFieldsCorrectly() {
            ClientName name = ClientName.of("Jane Smith");
            ClientEmail clientEmail = ClientEmail.of("jane@example.com");
            ClientPhoneNumber phone = ClientPhoneNumber.of("+33987654321");
            PersonBirthDate birthDate = PersonBirthDate.of(LocalDate.of(1985, 6, 20));

            when(clientRepository.existsByEmail(clientEmail)).thenReturn(false);
            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Person result = service.createAndPersistPerson(name, clientEmail, phone, birthDate);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getEmail()).isEqualTo(clientEmail);
            assertThat(result.getPhone()).isEqualTo(phone);
            assertThat(result.getBirthDate()).isEqualTo(birthDate);
        }
    }

    @Nested
    @DisplayName("Create Company - Business Rules")
    class CreateCompanyTests {

        @Test
        @DisplayName("Should create Company when email and identifier are unique")
        void shouldCreateCompanyWhenAllUnique() {
            ClientName name = ClientName.of("Acme Corp");
            ClientEmail clientEmail = ClientEmail.of("contact@acme.com");
            ClientPhoneNumber phone = ClientPhoneNumber.of("+33123456789");
            CompanyIdentifier companyId = CompanyIdentifier.of("CHE-123.456.789");

            when(clientRepository.existsByEmail(clientEmail)).thenReturn(false);
            when(clientRepository.existsByCompanyIdentifier(companyId)).thenReturn(false);
            when(clientRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Company result = service.createAndPersistCompany(name, clientEmail, phone, companyId);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getEmail()).isEqualTo(clientEmail);
            assertThat(result.getPhone()).isEqualTo(phone);
            assertThat(result.getCompanyIdentifier()).isEqualTo(companyId);

            verify(clientRepository).existsByEmail(clientEmail);
            verify(clientRepository).existsByCompanyIdentifier(companyId);
            verify(clientRepository).save(any(Company.class));
        }

        @Test
        @DisplayName("Should throw ClientAlreadyExistsException when email already exists")
        void shouldThrowWhenEmailAlreadyExists() {
            ClientName name = ClientName.of("Acme Corp");
            ClientEmail clientEmail = ClientEmail.of("duplicate@acme.com");
            ClientPhoneNumber phone = ClientPhoneNumber.of("+33123456789");
            CompanyIdentifier companyId = CompanyIdentifier.of("CHE-123.456.789");

            when(clientRepository.existsByEmail(clientEmail)).thenReturn(true);

            assertThatThrownBy(() -> service.createAndPersistCompany(name, clientEmail, phone, companyId))
                    .isInstanceOf(ClientAlreadyExistsException.class)
                    .hasMessageContaining("Client already exists")
                    .extracting("businessKey")
                    .isEqualTo(clientEmail.value());

            verify(clientRepository).existsByEmail(clientEmail);
        }

        @Test
        @DisplayName("Should throw CompanyIdentifierAlreadyExistsException when identifier already exists")
        void shouldThrowWhenCompanyIdentifierAlreadyExists() {
            ClientName name = ClientName.of("Acme Corp");
            ClientEmail clientEmail = ClientEmail.of("contact@acme.com");
            ClientPhoneNumber phone = ClientPhoneNumber.of("+33123456789");
            CompanyIdentifier companyId = CompanyIdentifier.of("CHE-999.888.777");

            when(clientRepository.existsByEmail(clientEmail)).thenReturn(false);
            when(clientRepository.existsByCompanyIdentifier(companyId)).thenReturn(true);

            assertThatThrownBy(() -> service.createAndPersistCompany(name, clientEmail, phone, companyId))
                    .isInstanceOf(CompanyIdentifierAlreadyExistsException.class)
                    .hasMessageContaining("already exists")
                    .hasMessageContaining(companyId.value());

            verify(clientRepository).existsByEmail(clientEmail);
            verify(clientRepository).existsByCompanyIdentifier(companyId);
        }

        @Test
        @DisplayName("Should create Company with all fields correctly mapped")
        void shouldMapAllFieldsCorrectly() {
            ClientName name = ClientName.of("Tech Solutions SA");
            ClientEmail clientEmail = ClientEmail.of("info@techsolutions.ch");
            ClientPhoneNumber phone = ClientPhoneNumber.of("+41223456789");
            CompanyIdentifier companyId = CompanyIdentifier.of("CHE-111.222.333");

            when(clientRepository.existsByEmail(clientEmail)).thenReturn(false);
            when(clientRepository.existsByCompanyIdentifier(companyId)).thenReturn(false);
            when(clientRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Company result = service.createAndPersistCompany(name, clientEmail, phone, companyId);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getEmail()).isEqualTo(clientEmail);
            assertThat(result.getPhone()).isEqualTo(phone);
            assertThat(result.getCompanyIdentifier()).isEqualTo(companyId);
        }
    }

    @Nested
    @DisplayName("Ensure Email Is Unique - Business Rule")
    class EnsureClientEmailIsUniqueTests {

        @Test
        @DisplayName("Should not throw when email is unique")
        void shouldNotThrowWhenEmailIsUnique() {
            ClientEmail clientEmail = ClientEmail.of("unique@example.com");

            when(clientRepository.existsByEmail(clientEmail)).thenReturn(false);

            service.ensureEmailIsUnique(clientEmail);

            verify(clientRepository).existsByEmail(clientEmail);
        }

        @Test
        @DisplayName("Should throw when email already exists")
        void shouldThrowWhenEmailExists() {
            ClientEmail clientEmail = ClientEmail.of("existing@example.com");

            when(clientRepository.existsByEmail(clientEmail)).thenReturn(true);

            assertThatThrownBy(() -> service.ensureEmailIsUnique(clientEmail))
                    .isInstanceOf(ClientAlreadyExistsException.class)
                    .hasMessageContaining("Client already exists");

            verify(clientRepository).existsByEmail(clientEmail);
        }
    }

    @Nested
    @DisplayName("Ensure Company Identifier Is Unique - Business Rule")
    class EnsureCompanyIdentifierIsUniqueTests {

        @Test
        @DisplayName("Should not throw when company identifier is unique")
        void shouldNotThrowWhenIdentifierIsUnique() {
            CompanyIdentifier identifier = CompanyIdentifier.of("CHE-123.456.789");

            when(clientRepository.existsByCompanyIdentifier(identifier)).thenReturn(false);

            service.ensureCompanyIdentifierIsUnique(identifier);

            verify(clientRepository).existsByCompanyIdentifier(identifier);
        }

        @Test
        @DisplayName("Should throw when company identifier already exists")
        void shouldThrowWhenIdentifierExists() {
            CompanyIdentifier identifier = CompanyIdentifier.of("CHE-999.888.777");

            when(clientRepository.existsByCompanyIdentifier(identifier)).thenReturn(true);

            assertThatThrownBy(() -> service.ensureCompanyIdentifierIsUnique(identifier))
                    .isInstanceOf(CompanyIdentifierAlreadyExistsException.class)
                    .hasMessageContaining("already exists")
                    .hasMessageContaining(identifier.value());

            verify(clientRepository).existsByCompanyIdentifier(identifier);
        }
    }
}

