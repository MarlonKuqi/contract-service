package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.contract.usecase.CloseActiveContractsUseCase.CloseActiveContractsCommand;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloseActiveContractsUseCase - Unit Tests")
class CloseActiveContractsUseCaseImplTest {

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private CloseActiveContractsUseCaseImpl closeActiveContractsUseCase;

    @Nested
    @DisplayName("execute() - Happy Path")
    class ExecuteHappyPath {

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should close all active contracts for client")
        void shouldCloseAllActiveContracts() {
            // Given
            UUID clientId = UUID.randomUUID();
            CloseActiveContractsCommand command = new CloseActiveContractsCommand(clientId);

            doNothing().when(contractRepository).closeAllActiveByClientId(clientId);

            // When
            closeActiveContractsUseCase.execute(command);

            // Then
            verify(contractRepository).closeAllActiveByClientId(clientId);
        }

        @Test
        @DisplayName("GIVEN command with specific client WHEN execute THEN should close contracts only for that client")
        void shouldCloseContractsForSpecificClientOnly() {
            // Given
            UUID clientId = UUID.randomUUID();
            CloseActiveContractsCommand command = new CloseActiveContractsCommand(clientId);

            doNothing().when(contractRepository).closeAllActiveByClientId(clientId);

            // When
            closeActiveContractsUseCase.execute(command);

            // Then
            ArgumentCaptor<UUID> clientIdCaptor = ArgumentCaptor.forClass(UUID.class);
            verify(contractRepository).closeAllActiveByClientId(clientIdCaptor.capture());

            assertThat(clientIdCaptor.getValue()).isEqualTo(clientId);
        }

        @Test
        @DisplayName("GIVEN multiple commands WHEN execute THEN should close contracts for each client separately")
        void shouldCloseContractsForEachClientSeparately() {
            // Given
            UUID clientId1 = UUID.randomUUID();
            UUID clientId2 = UUID.randomUUID();
            UUID clientId3 = UUID.randomUUID();

            CloseActiveContractsCommand command1 = new CloseActiveContractsCommand(clientId1);
            CloseActiveContractsCommand command2 = new CloseActiveContractsCommand(clientId2);
            CloseActiveContractsCommand command3 = new CloseActiveContractsCommand(clientId3);

            doNothing().when(contractRepository).closeAllActiveByClientId(any(UUID.class));

            // When
            closeActiveContractsUseCase.execute(command1);
            closeActiveContractsUseCase.execute(command2);
            closeActiveContractsUseCase.execute(command3);

            // Then
            verify(contractRepository).closeAllActiveByClientId(clientId1);
            verify(contractRepository).closeAllActiveByClientId(clientId2);
            verify(contractRepository).closeAllActiveByClientId(clientId3);
            verify(contractRepository, times(3)).closeAllActiveByClientId(any(UUID.class));
        }
    }

    @Nested
    @DisplayName("execute() - Repository Interaction")
    class ExecuteRepositoryInteraction {

        @Test
        @DisplayName("GIVEN valid command WHEN execute THEN should call repository once")
        void shouldCallRepositoryOnce() {
            // Given
            UUID clientId = UUID.randomUUID();
            CloseActiveContractsCommand command = new CloseActiveContractsCommand(clientId);

            doNothing().when(contractRepository).closeAllActiveByClientId(clientId);

            // When
            closeActiveContractsUseCase.execute(command);

            // Then
            verify(contractRepository, times(1)).closeAllActiveByClientId(clientId);
        }

        @Test
        @DisplayName("GIVEN same client ID multiple times WHEN execute THEN should call repository each time")
        void shouldCallRepositoryForEachExecution() {
            // Given
            UUID clientId = UUID.randomUUID();
            CloseActiveContractsCommand command = new CloseActiveContractsCommand(clientId);

            doNothing().when(contractRepository).closeAllActiveByClientId(clientId);

            // When
            closeActiveContractsUseCase.execute(command);
            closeActiveContractsUseCase.execute(command);

            // Then
            verify(contractRepository, times(2)).closeAllActiveByClientId(clientId);
        }

        @Test
        @DisplayName("GIVEN command WHEN execute THEN should delegate to repository without transformation")
        void shouldDelegateToRepositoryWithoutTransformation() {
            // Given
            UUID clientId = UUID.randomUUID();
            CloseActiveContractsCommand command = new CloseActiveContractsCommand(clientId);

            doNothing().when(contractRepository).closeAllActiveByClientId(any(UUID.class));

            // When
            closeActiveContractsUseCase.execute(command);

            // Then
            ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
            verify(contractRepository).closeAllActiveByClientId(captor.capture());

            // The UUID passed to repository should be exactly the same as in the command
            assertThat(captor.getValue()).isSameAs(command.clientId());
        }
    }

    @Nested
    @DisplayName("execute() - Edge Cases")
    class ExecuteEdgeCases {

        @Test
        @DisplayName("GIVEN client with no active contracts WHEN execute THEN should complete without error")
        void shouldCompleteSuccessfullyWhenNoActiveContracts() {
            // Given
            UUID clientId = UUID.randomUUID();
            CloseActiveContractsCommand command = new CloseActiveContractsCommand(clientId);

            // Repository doesn't throw exception when no contracts found
            doNothing().when(contractRepository).closeAllActiveByClientId(clientId);

            // When
            closeActiveContractsUseCase.execute(command);

            // Then
            verify(contractRepository).closeAllActiveByClientId(clientId);
        }

        @Test
        @DisplayName("GIVEN client with only closed contracts WHEN execute THEN should complete without error")
        void shouldCompleteSuccessfullyWhenOnlyClosedContracts() {
            // Given
            UUID clientId = UUID.randomUUID();
            CloseActiveContractsCommand command = new CloseActiveContractsCommand(clientId);

            doNothing().when(contractRepository).closeAllActiveByClientId(clientId);

            // When
            closeActiveContractsUseCase.execute(command);

            // Then
            verify(contractRepository).closeAllActiveByClientId(clientId);
        }

        @Test
        @DisplayName("GIVEN non-existent client WHEN execute THEN should delegate to repository")
        void shouldDelegateToRepositoryEvenForNonExistentClient() {
            // Given - A client that doesn't exist
            UUID nonExistentClientId = UUID.randomUUID();
            CloseActiveContractsCommand command = new CloseActiveContractsCommand(nonExistentClientId);

            doNothing().when(contractRepository).closeAllActiveByClientId(nonExistentClientId);

            // When
            closeActiveContractsUseCase.execute(command);

            // Then - Should still call repository, validation is repository's responsibility
            verify(contractRepository).closeAllActiveByClientId(nonExistentClientId);
        }
    }
}

