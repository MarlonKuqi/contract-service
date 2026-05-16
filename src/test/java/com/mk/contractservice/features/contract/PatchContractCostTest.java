package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchContractCost Handler")
class PatchContractCostTest {

    @Mock
    private ContractRepository contractRepository;

    private PatchContractCost.Handler patchContractCostHandler;

    @BeforeEach
    void setUp() {
        patchContractCostHandler = new PatchContractCost.Handler(contractRepository);
    }

    @Test
    @DisplayName("GIVEN contrat inexistant WHEN execute THEN lève ContractNotFoundException")
    void shouldThrowExceptionWhenContractNotFound() {
        // Given
        UUID nonExistentContractId = UUID.randomUUID();

        PatchContractCost.Command command = new PatchContractCost.Command(
                nonExistentContractId,
                new BigDecimal("1000.00")
        );

        when(contractRepository.findById(nonExistentContractId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> patchContractCostHandler.execute(command))
                .isInstanceOf(ContractNotFoundException.class);
    }
}
