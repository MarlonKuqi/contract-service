package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientValidationService;
import com.mk.contractservice.domain.client.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

