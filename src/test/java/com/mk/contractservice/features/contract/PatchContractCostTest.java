package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFactory;
import com.mk.contractservice.domain.contract.ContractRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

        when(contractRepository.getById(contractId)).thenReturn(expiredContract);

        assertThatThrownBy(() -> patchContractCostHandler.execute(
                new PatchContractCost.Command(contractId, new BigDecimal("1000.00"))))
                .isInstanceOf(ExpiredContractException.class);
    }

    @Test
    @DisplayName("GIVEN même montant avec une autre échelle WHEN execute THEN ne sauvegarde pas et renvoie le contrat existant")
    void shouldNotSaveWhenCostIsTheSameAmount() {
        UUID contractId = UUID.randomUUID();
        Contract activeContract = activeContractWithCost(contractId, new BigDecimal("500.00"));

        when(contractRepository.getById(contractId)).thenReturn(activeContract);

        Contract result = patchContractCostHandler.execute(
                new PatchContractCost.Command(contractId, new BigDecimal("500.0")));

        verify(contractRepository, never()).save(any(Contract.class));
        assertThat(result).isSameAs(activeContract);
    }

    @Test
    @DisplayName("GIVEN un nouveau montant WHEN execute THEN sauvegarde le contrat modifié")
    void shouldSaveWhenCostChanges() {
        UUID contractId = UUID.randomUUID();
        Contract activeContract = activeContractWithCost(contractId, new BigDecimal("500.00"));

        when(contractRepository.getById(contractId)).thenReturn(activeContract);
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contract result = patchContractCostHandler.execute(
                new PatchContractCost.Command(contractId, new BigDecimal("1000.00")));

        verify(contractRepository).save(result);
        assertThat(result.getCostAmount().getValue()).isEqualByComparingTo("1000.00");
        assertThat(result.getLastModified()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
    }

    private static Contract activeContractWithCost(final UUID contractId, final BigDecimal cost) {
        LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
        return ContractFactory.buildFromDatabase(
                contractId,
                UUID.randomUUID(),
                now.minusMonths(6),
                null,
                cost,
                now.minusMonths(6)
        );
    }
}
