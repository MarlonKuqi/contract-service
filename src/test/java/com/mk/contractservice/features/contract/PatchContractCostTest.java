package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFactory;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import com.mk.contractservice.domain.contract.exception.ExpiredContractException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchContractCost Handler")
class PatchContractCostTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ContractRepository contractRepository;

    private PatchContractCost.Handler patchContractCostHandler;

    @BeforeEach
    void setUp() {
        patchContractCostHandler = new PatchContractCost.Handler(contractRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("GIVEN contrat inexistant WHEN execute THEN lève ContractNotFoundException")
    void shouldThrowExceptionWhenContractNotFound() {
        UUID nonExistentContractId = UUID.randomUUID();

        PatchContractCost.Command command = new PatchContractCost.Command(
                nonExistentContractId,
                new BigDecimal("1000.00")
        );

        when(contractRepository.findById(nonExistentContractId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> patchContractCostHandler.execute(command))
                .isInstanceOf(ContractNotFoundException.class);
    }

    @Test
    @DisplayName("GIVEN contrat expiré à l'heure de l'horloge WHEN execute THEN lève ExpiredContractException")
    void shouldRejectCostChangeWhenContractExpiredAtClockTime() {
        UUID contractId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
        Contract expiredContract = ContractFactory.buildFromDatabase(
                contractId,
                UUID.randomUUID(),
                now.minusMonths(6),
                now.minusSeconds(1),
                new BigDecimal("500.00"),
                now.minusMonths(6)
        );

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(expiredContract));

        assertThatThrownBy(() -> patchContractCostHandler.execute(
                new PatchContractCost.Command(contractId, new BigDecimal("1000.00"))))
                .isInstanceOf(ExpiredContractException.class);
    }
}
