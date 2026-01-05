package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.feature.contract.create.CreateContract;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientValidationService;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateContract Handler")
class CreateContractTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractRepository contractRepository;

    private ClientValidationService clientValidationService;
    private CreateContract.Handler createContractHandler;

    @BeforeEach
    void setUp() {
        clientValidationService = new ClientValidationService(clientRepository);
        createContractHandler = new CreateContract.Handler(clientValidationService, contractRepository);
    }

    @Nested
    @DisplayName("Création de contrat")
    class CreateContractSuccess {

        @BeforeEach
        void setUp() {
            when(clientRepository.existsById(any(UUID.class))).thenReturn(true);
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> {
                Contract contract = invocation.getArgument(0);
                return contract.getId() == null
                        ? Contract.builder()
                        .id(UUID.randomUUID())
                        .clientId(contract.getClientId())
                        .period(contract.getPeriod())
                        .costAmount(contract.getCostAmount())
                        .build()
                        : contract;
            });
        }

        @Test
        @DisplayName("GIVEN commande valide WHEN execute THEN crée le contrat avec tous les champs")
        void shouldCreateContractWithAllFields() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);
            BigDecimal costAmount = new BigDecimal("1500.00");

            CreateContract.Command command = new CreateContract.Command(
                    clientId,
                    startDate,
                    endDate,
                    costAmount
            );

            // When
            Contract result = createContractHandler.execute(command);

            // Then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getClientId()).isEqualTo(command.clientId());
            assertThat(result.getPeriod().getStartDate()).isEqualTo(command.startDate());
            assertThat(result.getPeriod().getEndDate()).isEqualTo(command.endDate());
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(command.costAmount());
        }
    }

    @Nested
    @DisplayName("Erreurs de validation")
    class ValidationErrors {

        @Test
        @DisplayName("GIVEN client inexistant WHEN execute THEN lève ClientNotFoundException")
        void shouldThrowExceptionWhenClientNotFound() {
            // Given
            UUID nonExistentClientId = UUID.randomUUID();
            CreateContract.Command command = new CreateContract.Command(
                    nonExistentClientId,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusMonths(12),
                    new BigDecimal("1000.00")
            );

            when(clientRepository.existsById(nonExistentClientId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> createContractHandler.execute(command))
                    .isInstanceOf(ClientNotFoundException.class);
        }
    }
}
