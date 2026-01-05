package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.feature.contract.patchcost.PatchContractCost;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.service.ContractService;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
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
@DisplayName("PatchContractCost Handler")
class PatchContractCostTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractService contractService;

    private PatchContractCost.Handler patchContractCostHandler;

    @BeforeEach
    void setUp() {
        patchContractCostHandler = new PatchContractCost.Handler(contractRepository, contractService);
    }

    @Nested
    @DisplayName("Mise à jour du coût")
    class UpdateCostSuccess {

        @BeforeEach
        void setUp() {
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }
        @Test
        @DisplayName("GIVEN commande valide WHEN execute THEN met à jour le coût du contrat")
        void shouldUpdateContractCost() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime endDate = null;
            BigDecimal oldCost = new BigDecimal("1000.00");
            BigDecimal newCost = new BigDecimal("1500.00");

            Contract existingContract = Contract.reconstituteFromDatabase(
                    contractId,
                    clientId,
                    ContractPeriod.of(startDate, endDate),
                    ContractCost.of(oldCost)
            );

            PatchContractCost.Command command = new PatchContractCost.Command(
                    clientId,
                    contractId,
                    newCost
            );

            when(contractService.getContractForClient(clientId, contractId))
                    .thenReturn(existingContract);

            // When
            Contract result = patchContractCostHandler.execute(command);

            // Then
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(command.newCostAmount());
            assertThat(result.getClientId()).isEqualTo(existingContract.getClientId());
            assertThat(result.getPeriod().getStartDate()).isEqualTo(existingContract.getPeriod().getStartDate());
            assertThat(result.getPeriod().getEndDate()).isEqualTo(existingContract.getPeriod().getEndDate());
        }

        @Test
        @DisplayName("GIVEN coût augmenté WHEN execute THEN met à jour avec le nouveau montant")
        void shouldUpdateWithIncreasedCost() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            BigDecimal oldCost = new BigDecimal("500.00");
            BigDecimal newCost = new BigDecimal("750.00");

            Contract existingContract = Contract.reconstituteFromDatabase(
                    contractId,
                    clientId,
                    ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusMonths(6)),
                    ContractCost.of(oldCost)
            );

            PatchContractCost.Command command = new PatchContractCost.Command(
                    clientId,
                    contractId,
                    newCost
            );

            when(contractService.getContractForClient(clientId, contractId))
                    .thenReturn(existingContract);

            // When
            Contract result = patchContractCostHandler.execute(command);

            // Then
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(command.newCostAmount());
        }

        @Test
        @DisplayName("GIVEN coût diminué WHEN execute THEN met à jour avec le nouveau montant")
        void shouldUpdateWithDecreasedCost() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            BigDecimal oldCost = new BigDecimal("2000.00");
            BigDecimal newCost = new BigDecimal("1500.00");

            Contract existingContract = Contract.reconstituteFromDatabase(
                    contractId,
                    clientId,
                    ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusMonths(12)),
                    ContractCost.of(oldCost)
            );

            PatchContractCost.Command command = new PatchContractCost.Command(
                    clientId,
                    contractId,
                    newCost
            );

            when(contractService.getContractForClient(clientId, contractId))
                    .thenReturn(existingContract);

            // When
            Contract result = patchContractCostHandler.execute(command);

            // Then
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(command.newCostAmount());
        }
    }

    @Nested
    @DisplayName("Erreurs de validation")
    class ValidationErrors {

        @Test
        @DisplayName("GIVEN contrat inexistant WHEN execute THEN lève ContractNotFoundException")
        void shouldThrowExceptionWhenContractNotFound() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID nonExistentContractId = UUID.randomUUID();

            PatchContractCost.Command command = new PatchContractCost.Command(
                    clientId,
                    nonExistentContractId,
                    new BigDecimal("1000.00")
            );

            when(contractService.getContractForClient(clientId, nonExistentContractId))
                    .thenThrow(new ContractNotFoundException(nonExistentContractId));

            // When & Then
            assertThatThrownBy(() -> patchContractCostHandler.execute(command))
                    .isInstanceOf(ContractNotFoundException.class);
        }
    }
}

