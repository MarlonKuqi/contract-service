package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.client.exception.InvalidPersonBirthDateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePerson Handler")
class CreatePersonHandlerTest {

    @Mock
    private ClientRepository clientRepository;

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    private CreatePerson.Handler createPersonHandler;

    @BeforeEach
    void setUp() {
        createPersonHandler = new CreatePerson.Handler(clientRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("GIVEN date de naissance postérieure à la date de l'horloge WHEN execute THEN lève InvalidPersonBirthDateException")
    void shouldRejectBirthDateAfterClockDate() {
        CreatePerson.Command command = new CreatePerson.Command(
                "John Doe",
                "john@example.com",
                "+33123456789",
                LocalDate.now(FIXED_CLOCK).plusDays(1)
        );

        assertThatThrownBy(() -> createPersonHandler.execute(command))
                .isInstanceOf(InvalidPersonBirthDateException.class);
    }

    @Test
    @DisplayName("GIVEN date de naissance égale à la date de l'horloge WHEN execute THEN sauvegarde la personne")
    void shouldAcceptBirthDateEqualToClockDate() {
        LocalDate today = LocalDate.now(FIXED_CLOCK);
        CreatePerson.Command command = new CreatePerson.Command("John Doe", "john@example.com", "+33123456789", today);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Person result = createPersonHandler.execute(command);

        assertThat(result.getBirthDate().getValue()).isEqualTo(today);
    }

    @Test
    @DisplayName("GIVEN commande valide WHEN execute THEN sauvegarde la personne")
    void shouldSavePersonWhenCommandIsValid() {
        CreatePerson.Command command = new CreatePerson.Command(
                "John Doe",
                "john@example.com",
                "+33123456789",
                LocalDate.of(1990, 5, 15)
        );
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Person result = createPersonHandler.execute(command);

        assertThat(result.getName().getValue()).isEqualTo("John Doe");
        assertThat(result.getEmail().getValue()).isEqualTo("john@example.com");
        assertThat(result.getBirthDate().getValue()).isEqualTo(LocalDate.of(1990, 5, 15));
    }
}
