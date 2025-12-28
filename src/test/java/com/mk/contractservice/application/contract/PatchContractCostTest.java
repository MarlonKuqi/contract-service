package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.feature.contract.patchcost.PatchContractCost;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.service.ContractService;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchContractCost - Unit Tests")
class PatchContractCostTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractService contractService;

    @InjectMocks
    private PatchContractCost.Handler patchContractCost;

    @Nested
    @DisplayName("execute() - Happy Path")
    class ExecuteHappyPath {

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should update contract cost")
        void shouldUpdateContractCost() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            BigDecimal oldCost = new BigDecimal("1000.00");
            BigDecimal newCost = new BigDecimal("1500.00");

            Contract existingContract = Contract.reconstituteFromDatabase(
                    contractId,
                    clientId,
                    ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusMonths(12)),
                    ContractCost.of(oldCost)
            );

            Contract updatedContract = existingContract.changeCost(ContractCost.of(newCost));

            PatchContractCost.Command command = new PatchContractCost.Command(
                    clientId,
                    contractId,
                    newCost
            );

            when(contractService.getContractForClient(clientId, contractId))
                    .thenReturn(existingContract);
            when(contractRepository.save(any(Contract.class))).thenReturn(updatedContract);

            // When
            Contract result = patchContractCost.execute(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(newCost);

            verify(contractService).getContractForClient(clientId, contractId);
            verify(contractRepository).save(any(Contract.class));
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should preserve other contract fields")
        void shouldPreserveOtherContractFields() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(12);
            BigDecimal oldCost = new BigDecimal("1000.00");
            BigDecimal newCost = new BigDecimal("2000.00");

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
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Contract result = patchContractCost.execute(command);

            // Then
            assertThat(result.getClientId()).isEqualTo(clientId);
            assertThat(result.getPeriod().getStartDate()).isEqualTo(startDate);
            assertThat(result.getPeriod().getEndDate()).isEqualTo(endDate);
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(newCost);
        }

        @Test
        @DisplayName("GIVEN command with increased cost WHEN execute THEN should save updated contract")
        void shouldSaveUpdatedContractWithIncreasedCost() {
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
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            patchContractCost.execute(command);

            // Then
            ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
            verify(contractRepository).save(contractCaptor.capture());

            Contract savedContract = contractCaptor.getValue();
            assertThat(savedContract.getCostAmount().getValue()).isEqualByComparingTo(newCost);
        }

        @Test
        @DisplayName("GIVEN command with decreased cost WHEN execute THEN should save updated contract")
        void shouldSaveUpdatedContractWithDecreasedCost() {
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
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Contract result = patchContractCost.execute(command);

            // Then
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(newCost);
            verify(contractRepository).save(any(Contract.class));
        }
    }

    @Nested
    @DisplayName("execute() - Validation Errors")
    class ExecuteValidationErrors {

        @Test
        @DisplayName("GIVEN non-existent contract WHEN execute THEN should throw ContractNotFoundException")
        void shouldThrowExceptionWhenContractDoesNotExist() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            BigDecimal newCost = new BigDecimal("1500.00");

            PatchContractCost.Command command = new PatchContractCost.Command(
                    clientId,
                    contractId,
                    newCost
            );

            when(contractService.getContractForClient(clientId, contractId))
                    .thenThrow(new ContractNotFoundException(contractId));

            // When & Then
            assertThatThrownBy(() -> patchContractCost.execute(command))
                    .isInstanceOf(ContractNotFoundException.class);

            verify(contractService).getContractForClient(clientId, contractId);
            verify(contractRepository, never()).save(any(Contract.class));
        }

        @Test
        @DisplayName("GIVEN wrong client ID WHEN execute THEN should throw exception")
        void shouldThrowExceptionWhenWrongClientId() {
            // Given
            UUID wrongClientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            BigDecimal newCost = new BigDecimal("1500.00");

            PatchContractCost.Command command = new PatchContractCost.Command(
                    wrongClientId,
                    contractId,
                    newCost
            );

            when(contractService.getContractForClient(wrongClientId, contractId))
                    .thenThrow(new ContractNotFoundException(contractId));

            // When & Then
            assertThatThrownBy(() -> patchContractCost.execute(command))
                    .isInstanceOf(ContractNotFoundException.class);

            verify(contractService).getContractForClient(wrongClientId, contractId);
            verify(contractRepository, never()).save(any(Contract.class));
        }

        @Test
        @DisplayName("GIVEN command WHEN execute THEN should retrieve contract before updating")
        void shouldRetrieveContractBeforeUpdating() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            BigDecimal newCost = new BigDecimal("1500.00");

            Contract existingContract = Contract.reconstituteFromDatabase(
                    contractId,
                    clientId,
                    ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusMonths(12)),
                    ContractCost.of(new BigDecimal("1000.00"))
            );

            PatchContractCost.Command command = new PatchContractCost.Command(
                    clientId,
                    contractId,
                    newCost
            );

            when(contractService.getContractForClient(clientId, contractId))
                    .thenReturn(existingContract);
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            patchContractCost.execute(command);

            // Then
            var ordered = inOrder(contractService, contractRepository);
            ordered.verify(contractService).getContractForClient(clientId, contractId);
            ordered.verify(contractRepository).save(any(Contract.class));
        }
    }

    @Nested
    @DisplayName("execute() - Edge Cases")
    class ExecuteEdgeCases {

        @Test
        @DisplayName("GIVEN minimum cost WHEN execute THEN should update contract successfully")
        void shouldUpdateToMinimumCost() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            BigDecimal newCost = new BigDecimal("0.01");

            Contract existingContract = Contract.reconstituteFromDatabase(
                    contractId,
                    clientId,
                    ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusMonths(12)),
                    ContractCost.of(new BigDecimal("1000.00"))
            );

            PatchContractCost.Command command = new PatchContractCost.Command(
                    clientId,
                    contractId,
                    newCost
            );

            when(contractService.getContractForClient(clientId, contractId))
                    .thenReturn(existingContract);
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Contract result = patchContractCost.execute(command);

            // Then
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(newCost);
        }

        @Test
        @DisplayName("GIVEN large cost amount WHEN execute THEN should update contract successfully")
        void shouldUpdateToLargeCost() {
            // Given
            UUID clientId = UUID.randomUUID();
            UUID contractId = UUID.randomUUID();
            BigDecimal newCost = new BigDecimal("999999.99");

            Contract existingContract = Contract.reconstituteFromDatabase(
                    contractId,
                    clientId,
                    ContractPeriod.of(LocalDateTime.now(), LocalDateTime.now().plusMonths(12)),
                    ContractCost.of(new BigDecimal("1000.00"))
            );

            PatchContractCost.Command command = new PatchContractCost.Command(
                    clientId,
                    contractId,
                    newCost
            );

            when(contractService.getContractForClient(clientId, contractId))
                    .thenReturn(existingContract);
            when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Contract result = patchContractCost.execute(command);

            // Then
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(newCost);
        }
    }
}

