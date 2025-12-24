package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.contract.usecase.CreateContractUseCase.CreateContractCommand;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.service.ContractValidationService;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateContractUseCase - Unit Tests")
class CreateContractUseCaseImplTest {

    @Mock
    private ContractValidationService contractValidationService;

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private CreateContractUseCaseImpl createContractUseCase;

    @Nested
    @DisplayName("execute() - Happy Path")
    class ExecuteHappyPath {

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should create and save contract")
        void shouldCreateAndSaveContract() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(12);
            BigDecimal costAmount = new BigDecimal("1500.00");

            CreateContractCommand command = new CreateContractCommand(
                    clientId,
                    startDate,
                    endDate,
                    costAmount
            );

            Contract expectedContract = Contract.of(
                    clientId,
                    ContractPeriod.of(startDate, endDate),
                    ContractCost.of(costAmount)
            );

            doNothing().when(contractValidationService).ensureClientExists(clientId);
            when(contractRepository.save(any(Contract.class))).thenReturn(expectedContract);

            // When
            Contract result = createContractUseCase.execute(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getClientId()).isEqualTo(clientId);
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(costAmount);

            verify(contractValidationService).ensureClientExists(clientId);
            verify(contractRepository).save(any(Contract.class));
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should create contract with correct period")
        void shouldCreateContractWithCorrectPeriod() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);
            BigDecimal costAmount = new BigDecimal("2000.00");

            CreateContractCommand command = new CreateContractCommand(
                    clientId,
                    startDate,
                    endDate,
                    costAmount
            );

            Contract savedContract = Contract.of(
                    clientId,
                    ContractPeriod.of(startDate, endDate),
                    ContractCost.of(costAmount)
            );

            doNothing().when(contractValidationService).ensureClientExists(clientId);
            when(contractRepository.save(any(Contract.class))).thenReturn(savedContract);

            // When
            createContractUseCase.execute(command);

            // Then
            ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
            verify(contractRepository).save(contractCaptor.capture());

            Contract capturedContract = contractCaptor.getValue();
            assertThat(capturedContract.getPeriod().getStartDate()).isEqualTo(startDate);
            assertThat(capturedContract.getPeriod().getEndDate()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should create contract with correct cost")
        void shouldCreateContractWithCorrectCost() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(6);
            BigDecimal costAmount = new BigDecimal("999.99");

            CreateContractCommand command = new CreateContractCommand(
                    clientId,
                    startDate,
                    endDate,
                    costAmount
            );

            Contract savedContract = Contract.of(
                    clientId,
                    ContractPeriod.of(startDate, endDate),
                    ContractCost.of(costAmount)
            );

            doNothing().when(contractValidationService).ensureClientExists(clientId);
            when(contractRepository.save(any(Contract.class))).thenReturn(savedContract);

            // When
            createContractUseCase.execute(command);

            // Then
            ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
            verify(contractRepository).save(contractCaptor.capture());

            Contract capturedContract = contractCaptor.getValue();
            assertThat(capturedContract.getCostAmount().getValue()).isEqualByComparingTo(costAmount);
        }
    }

    @Nested
    @DisplayName("execute() - Validation Errors")
    class ExecuteValidationErrors {

        @Test
        @DisplayName("GIVEN non-existent client WHEN execute THEN should throw ClientNotFoundException")
        void shouldThrowExceptionWhenClientDoesNotExist() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(12);
            BigDecimal costAmount = new BigDecimal("1500.00");

            CreateContractCommand command = new CreateContractCommand(
                    clientId,
                    startDate,
                    endDate,
                    costAmount
            );

            doThrow(new ClientNotFoundException(clientId.toString()))
                    .when(contractValidationService).ensureClientExists(clientId);

            // When & Then
            assertThatThrownBy(() -> createContractUseCase.execute(command))
                    .isInstanceOf(ClientNotFoundException.class)
                    .hasMessageContaining(clientId.toString());

            verify(contractValidationService).ensureClientExists(clientId);
            verify(contractRepository, never()).save(any(Contract.class));
        }

        @Test
        @DisplayName("GIVEN command WHEN execute THEN should validate client before saving")
        void shouldValidateClientBeforeSaving() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(12);
            BigDecimal costAmount = new BigDecimal("1500.00");

            CreateContractCommand command = new CreateContractCommand(
                    clientId,
                    startDate,
                    endDate,
                    costAmount
            );

            Contract savedContract = Contract.of(
                    clientId,
                    ContractPeriod.of(startDate, endDate),
                    ContractCost.of(costAmount)
            );

            doNothing().when(contractValidationService).ensureClientExists(clientId);
            when(contractRepository.save(any(Contract.class))).thenReturn(savedContract);

            // When
            createContractUseCase.execute(command);

            // Then
            var ordered = inOrder(contractValidationService, contractRepository);
            ordered.verify(contractValidationService).ensureClientExists(clientId);
            ordered.verify(contractRepository).save(any(Contract.class));
        }
    }

    @Nested
    @DisplayName("execute() - Edge Cases")
    class ExecuteEdgeCases {

        @Test
        @DisplayName("GIVEN minimum cost WHEN execute THEN should create contract successfully")
        void shouldCreateContractWithMinimumCost() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(1);
            BigDecimal costAmount = new BigDecimal("0.01");

            CreateContractCommand command = new CreateContractCommand(
                    clientId,
                    startDate,
                    endDate,
                    costAmount
            );

            Contract savedContract = Contract.of(
                    clientId,
                    ContractPeriod.of(startDate, endDate),
                    ContractCost.of(costAmount)
            );

            doNothing().when(contractValidationService).ensureClientExists(clientId);
            when(contractRepository.save(any(Contract.class))).thenReturn(savedContract);

            // When
            Contract result = createContractUseCase.execute(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(costAmount);
            verify(contractRepository).save(any(Contract.class));
        }

        @Test
        @DisplayName("GIVEN large cost amount WHEN execute THEN should create contract successfully")
        void shouldCreateContractWithLargeCost() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusYears(5);
            BigDecimal costAmount = new BigDecimal("999999.99");

            CreateContractCommand command = new CreateContractCommand(
                    clientId,
                    startDate,
                    endDate,
                    costAmount
            );

            Contract savedContract = Contract.of(
                    clientId,
                    ContractPeriod.of(startDate, endDate),
                    ContractCost.of(costAmount)
            );

            doNothing().when(contractValidationService).ensureClientExists(clientId);
            when(contractRepository.save(any(Contract.class))).thenReturn(savedContract);

            // When
            Contract result = createContractUseCase.execute(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCostAmount().getValue()).isEqualByComparingTo(costAmount);
            verify(contractRepository).save(any(Contract.class));
        }
    }
}

