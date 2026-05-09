package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.ClientValidationService;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractService;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListActiveContractsByClient Handler")
class ListActiveContractsByClientTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractRepository contractRepository;

    private ListActiveContractsByClient.Handler handler;

    @BeforeEach
    void setUp() {
        ClientValidationService clientValidationService = new ClientValidationService(clientRepository);
        ContractService contractService = new ContractService(contractRepository);
        handler = new ListActiveContractsByClient.Handler(clientValidationService, contractService);
    }

    @Test
    @DisplayName("GIVEN null clientId WHEN building Query THEN throw NullPointerException")
    void shouldThrowWhenClientIdIsNull() {
        assertThatThrownBy(() -> new ListActiveContractsByClient.Query(null, null, Pageable.unpaged()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("GIVEN null pageable WHEN building Query THEN throw NullPointerException")
    void shouldThrowWhenPageableIsNull() {
        assertThatThrownBy(() -> new ListActiveContractsByClient.Query(UUID.randomUUID(), null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("GIVEN non-existent clientId WHEN execute THEN throw ClientNotFoundException")
    void shouldThrowWhenClientNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(clientRepository.existsById(unknownId)).thenReturn(false);

        assertThatThrownBy(() -> handler.execute(new ListActiveContractsByClient.Query(unknownId, null, Pageable.unpaged())))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    @DisplayName("GIVEN existing client WHEN execute THEN return contracts page from ContractService")
    void shouldReturnContractsPageForExistingClient() {
        UUID clientId = UUID.randomUUID();
        Page<Contract> expectedPage = Page.empty();

        when(clientRepository.existsById(clientId)).thenReturn(true);
        when(contractRepository.findActiveByClientIdPageable(clientId, null, Pageable.unpaged()))
                .thenReturn(expectedPage);

        Page<Contract> result = handler.execute(new ListActiveContractsByClient.Query(clientId, null, Pageable.unpaged()));

        assertThat(result).isEqualTo(expectedPage);
    }
}

