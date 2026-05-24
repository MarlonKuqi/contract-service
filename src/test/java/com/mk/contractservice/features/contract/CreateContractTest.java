package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientValidationService;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
