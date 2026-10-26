package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFactory;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateContract Handler")
class CreateContractTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractRepository contractRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneId.of("Europe/Zurich"));

    private CreateContract.Handler createContractHandler;

    @BeforeEach
    void setUp() {
        createContractHandler = new CreateContract.Handler(clientRepository, contractRepository, clock);
    }

    @Test
    @DisplayName("GIVEN client existant WHEN execute THEN sauvegarde et retourne le contrat créé")
    void shouldSaveAndReturnCreatedContract() {
        // Given
        UUID clientId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(clock);
        CreateContract.Command command = new CreateContract.Command(
                clientId,
                now,
                now.plusMonths(12),
                new BigDecimal("1000.00")
        );
        Contract saved = ContractFactory.buildFromDatabase(UUID.randomUUID(), clientId, now, now.plusMonths(12), new BigDecimal("1000.00"), now);
        when(contractRepository.save(any(Contract.class))).thenReturn(saved);

        // When
        Contract result = createContractHandler.execute(command);

        // Then
        assertThat(result).isSameAs(saved);
        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo(clientId);
        assertThat(captor.getValue().getLastModified()).isEqualTo(now);
    }

    @Test
    @DisplayName("GIVEN client inexistant WHEN execute THEN lève ClientNotFoundException sans créer de contrat")
    void shouldThrowExceptionWhenClientNotFound() {
        // Given
        UUID nonExistentClientId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(clock);
        CreateContract.Command command = new CreateContract.Command(
                nonExistentClientId,
                now,
                now.plusMonths(12),
                new BigDecimal("1000.00")
        );

        doThrow(ClientNotFoundException.forId(nonExistentClientId)).when(clientRepository).ensureExists(nonExistentClientId);

        // When & Then
        assertThatThrownBy(() -> createContractHandler.execute(command))
                .isInstanceOf(ClientNotFoundException.class);
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    @DisplayName("GIVEN coût invalide WHEN execute THEN lève InvalidContractCostException sans interroger les clients")
    void shouldValidateContractBeforeCheckingClientExistence() {
        LocalDateTime now = LocalDateTime.now(clock);
        CreateContract.Command command = new CreateContract.Command(
                UUID.randomUUID(),
                now,
                now.plusMonths(12),
                new BigDecimal("-1000.00")
        );

        assertThatThrownBy(() -> createContractHandler.execute(command))
                .isInstanceOf(InvalidContractCostException.class);
        verify(clientRepository, never()).ensureExists(any(UUID.class));
        verify(contractRepository, never()).save(any(Contract.class));
    }
}
