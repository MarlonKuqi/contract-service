package com.mk.contractservice.application.client;

import com.mk.contractservice.application.feature.client.create.CreatePerson;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.exception.EmailAlreadyExistsException;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePerson Handler")
class CreatePersonHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    private ClientValidationService clientValidationService;
    private CreatePerson.Handler createPersonHandler;

    @BeforeEach
    void setUp() {
        clientValidationService = new ClientValidationService(clientRepository);
        createPersonHandler = new CreatePerson.Handler(clientRepository, clientValidationService);
    }

    @Nested
    @DisplayName("Création de personne")
    class CreatePersonTest {


        @Test
        @DisplayName("GIVEN commande valide WHEN execute THEN crée la personne avec tous les champs")
        void shouldCreatePersonWithAllFields() {
            // Given
            CreatePerson.Command command = new CreatePerson.Command(
                    "John Doe",
                    "john.doe@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            when(clientRepository.save(any(Person.class))).thenAnswer(invocation -> {
                Person person = invocation.getArgument(0);
                return Person.builder()
                        .id(UUID.randomUUID())
                        .name(person.getName())
                        .email(person.getEmail())
                        .phone(person.getPhone())
                        .birthDate(person.getBirthDate())
                        .build();
            });

            // When
            Person result = createPersonHandler.execute(command);

            // Then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getName().getValue()).isEqualTo(command.name());
            assertThat(result.getEmail().getValue()).isEqualTo(command.email());
            assertThat(result.getPhone().getValue()).isEqualTo(command.phoneNumber());
            assertThat(result.getBirthDate().getValue()).isEqualTo(command.birthDate());
        }
    }

    @Nested
    @DisplayName("Erreurs de validation")
    class ValidationErrors {

        @Test
        @DisplayName("GIVEN email déjà existant WHEN execute THEN lève EmailAlreadyExistsException")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // Given
            String duplicateEmail = "existing@example.com";
            CreatePerson.Command command = new CreatePerson.Command(
                    "John Doe",
                    duplicateEmail,
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            when(clientRepository.existsByEmail(duplicateEmail)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> createPersonHandler.execute(command))
                    .isInstanceOf(EmailAlreadyExistsException.class);
        }
    }
}

