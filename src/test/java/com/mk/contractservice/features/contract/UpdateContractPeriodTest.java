package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFixtures;
import com.mk.contractservice.domain.contract.ContractOverlapPolicy;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.exception.ExpiredContractException;
import com.mk.contractservice.domain.contract.exception.InvalidContractFranchiseException;
import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import com.mk.contractservice.domain.shared.exception.VersionMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateContractPeriod Handler")
class UpdateContractPeriodTest {

    private static final long VERSION = 3L;
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);
    private static final LocalDateTime CURRENT_START = NOW.minusMonths(6);
    private static final LocalDateTime CURRENT_END = NOW.plusMonths(6);

    private final UUID contractId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ClientRepository clientRepository;

    private UpdateContractPeriod.Handler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateContractPeriod.Handler(contractRepository,
                clientRepository,
                new ContractOverlapPolicy(contractRepository),
                FIXED_CLOCK);
    }

    @Test
    @DisplayName("GIVEN new start and end dates WHEN execute THEN saves the contract with the new period")
    void shouldSaveNewPeriod() {
        givenContract(CURRENT_START, CURRENT_END);
        givenSaveReturnsArgument();

        Contract result = handler.execute(command(NOW.minusMonths(7), NOW.plusYears(1)));

        verify(contractRepository).save(result);
        assertThat(result.period().startDate()).isEqualTo(NOW.minusMonths(7));
        assertThat(result.period().endDate()).isEqualTo(NOW.plusYears(1));
        assertThat(result.lastModified()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("GIVEN no end date WHEN execute THEN the contract becomes open-ended")
    void shouldRemoveEndDate() {
        givenContract(CURRENT_START, CURRENT_END);
        givenSaveReturnsArgument();

        Contract result = handler.execute(command(CURRENT_START, null));

        assertThat(result.period().startDate()).isEqualTo(CURRENT_START);
        assertThat(result.period().endDate()).isNull();
    }

    @Test
    @DisplayName("GIVEN same period WHEN execute THEN returns the loaded contract unchanged")
    void shouldNotSaveWhenPeriodIsTheSame() {
        Contract loaded = givenContract(CURRENT_START, CURRENT_END);

        Contract result = handler.execute(command(CURRENT_START, CURRENT_END));

        assertThat(result).isSameAs(loaded);
    }

    @Test
    @DisplayName("GIVEN a LAMal followed by another one WHEN its new period runs over the next one THEN refuses the new period")
    void shouldRefusePeriodOverlappingAnotherLamal() {
        Contract loaded = givenContract(CURRENT_START, CURRENT_END);
        Contract next = ContractFixtures.existingContract(UUID.randomUUID(),
                clientId,
                CURRENT_END,
                null,
                new BigDecimal("500.00"),
                CURRENT_START);
        when(contractRepository.lockAndFindAllByClientIdsAndTypes(Set.of(clientId), Set.of(ContractType.LAMAL)))
                .thenReturn(List.of(loaded, next));
        UpdateContractPeriod.Command command = command(CURRENT_START, CURRENT_END.plusMonths(1));

        assertThatThrownBy(() -> handler.execute(command)).isInstanceOf(OverlappingContractException.class);
    }

    @Test
    @DisplayName("GIVEN a LAMal WHEN its new period runs over its own previous period THEN saves it, the contract not overlapping itself")
    void shouldNotCompareTheContractWithItself() {
        Contract loaded = givenContract(CURRENT_START, CURRENT_END);
        when(contractRepository.lockAndFindAllByClientIdsAndTypes(Set.of(clientId), Set.of(ContractType.LAMAL)))
                .thenReturn(List.of(loaded));
        givenSaveReturnsArgument();

        Contract result = handler.execute(command(CURRENT_START, CURRENT_END.plusMonths(1)));

        assertThat(result.period().endDate()).isEqualTo(CURRENT_END.plusMonths(1));
    }

    @Test
    @DisplayName("GIVEN a child LAMal WHEN its end date is removed THEN refuses the new period: it must end with childhood")
    void shouldRecheckFranchiseAgainstTheHolderBirthDate() {
        LocalDate birthDate = LocalDate.of(2012, 3, 10);
        Contract childLamal = ContractFixtures.existingContract(contractId,
                clientId,
                ContractType.LAMAL,
                0,
                CURRENT_START,
                LocalDateTime.of(2031, 1, 1, 0, 0),
                new BigDecimal("500.00"),
                CURRENT_START);
        when(contractRepository.getById(contractId)).thenReturn(childLamal);
        when(clientRepository.getContractHolderIncludingDeleted(clientId))
                .thenReturn(ClientFixtures.existingPerson(clientId, birthDate).contractHolder());

        assertThatThrownBy(() -> handler.execute(command(CURRENT_START, null)))
                .isInstanceOf(InvalidContractFranchiseException.class);
    }

    @Test
    @DisplayName("GIVEN start date after end date WHEN execute THEN throws InvalidContractPeriodException")
    void shouldRejectStartAfterEnd() {
        givenContract(CURRENT_START, CURRENT_END);

        assertThatThrownBy(() -> handler.execute(command(CURRENT_END.plusDays(1), CURRENT_END)))
                .isInstanceOf(InvalidContractPeriodException.class);
    }

    @Test
    @DisplayName("GIVEN expired contract WHEN execute THEN throws ExpiredContractException")
    void shouldRejectExpiredContract() {
        givenContract(CURRENT_START, NOW.minusSeconds(1));

        assertThatThrownBy(() -> handler.execute(command(CURRENT_START, null)))
                .isInstanceOf(ExpiredContractException.class);
    }

    @Test
    @DisplayName("GIVEN new period WHEN execute THEN checks the expected version")
    void shouldCheckExpectedVersion() {
        givenContract(CURRENT_START, CURRENT_END);
        givenSaveReturnsArgument();

        handler.execute(command(CURRENT_START, null));

        verify(contractRepository).ensureVersion(contractId, VERSION);
    }

    @Test
    @DisplayName("GIVEN expired contract and stale version WHEN execute THEN the precondition is evaluated before the business rule")
    void shouldCheckVersionBeforeExpiration() {
        givenContract(CURRENT_START, NOW.minusSeconds(1));
        doThrow(new VersionMismatchException(VERSION, VERSION + 1)).when(contractRepository)
                .ensureVersion(contractId, VERSION);

        assertThatThrownBy(() -> handler.execute(command(CURRENT_START, null)))
                .isInstanceOf(VersionMismatchException.class);
    }

    @Test
    @DisplayName("GIVEN null contract id or start date WHEN building the command THEN throws NullPointerException")
    void shouldRejectNullMandatoryFields() {
        assertThatThrownBy(() -> new UpdateContractPeriod.Command(null, VERSION, CURRENT_START, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateContractPeriod.Command(contractId, VERSION, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    private UpdateContractPeriod.Command command(final LocalDateTime startDate, final LocalDateTime endDate) {
        return new UpdateContractPeriod.Command(contractId, VERSION, startDate, endDate);
    }

    private Contract givenContract(final LocalDateTime startDate, final LocalDateTime endDate) {
        Contract contract = ContractFixtures.existingContract(contractId,
                clientId,
                startDate,
                endDate,
                new BigDecimal("500.00"),
                CURRENT_START);
        lenient().when(clientRepository.getContractHolderIncludingDeleted(clientId))
                .thenReturn(ClientFixtures.existingPerson(clientId).contractHolder());
        when(contractRepository.getById(contractId)).thenReturn(contract);
        return contract;
    }

    private void givenSaveReturnsArgument() {
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
