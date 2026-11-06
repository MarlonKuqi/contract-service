package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.exception.ContractImportRejectedException;
import com.mk.contractservice.domain.contract.exception.InvalidImportSizeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImportContracts Handler")
class ImportContractsTest {

    private static final BigDecimal VALID_COST = new BigDecimal("1000.00");
    private static final BigDecimal NEGATIVE_COST = new BigDecimal("-1.00");

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ContractRepository contractRepository;

    @Captor
    private ArgumentCaptor<List<Contract>> insertedContracts;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    private final LocalDateTime now = LocalDateTime.now(clock);
    private final UUID clientId = UUID.randomUUID();

    private ImportContracts.Handler handler;

    @BeforeEach
    void setUp() {
        handler = new ImportContracts.Handler(clientRepository, contractRepository, clock);
    }

    private ImportContracts.Line validLine() {
        return new ImportContracts.Line(clientId, null, null, VALID_COST);
    }

    private ImportContracts.Command commandOf(final ImportContracts.Line... lines) {
        return new ImportContracts.Command(List.of(lines));
    }

    @Nested
    @DisplayName("Command")
    class CommandRules {

        @Test
        @DisplayName("GIVEN aucune ligne WHEN création THEN refuse l'import")
        void shouldRefuseEmptyImport() {
            final List<ImportContracts.Line> noLine = List.of();

            assertThatThrownBy(() -> new ImportContracts.Command(noLine))
                    .isInstanceOf(InvalidImportSizeException.class)
                    .hasMessageContaining("at least one");
        }

        @Test
        @DisplayName("GIVEN le nombre maximal de lignes WHEN création THEN accepte l'import")
        void shouldAcceptMaximumNumberOfLines() {
            final List<ImportContracts.Line> lines = Collections.nCopies(ImportContracts.MAX_LINES, validLine());

            assertThatCode(() -> new ImportContracts.Command(lines)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN une ligne de trop WHEN création THEN refuse l'import")
        void shouldRefuseImportAboveMaximum() {
            final List<ImportContracts.Line> lines = Collections.nCopies(ImportContracts.MAX_LINES + 1, validLine());

            assertThatThrownBy(() -> new ImportContracts.Command(lines))
                    .isInstanceOf(InvalidImportSizeException.class)
                    .hasMessageContaining(String.valueOf(ImportContracts.MAX_LINES));
        }

        @Test
        @DisplayName("GIVEN une ligne sans client WHEN création THEN refuse la ligne")
        void shouldRefuseLineWithoutClient() {
            assertThatThrownBy(() -> new ImportContracts.Line(null, null, null, VALID_COST))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("GIVEN une ligne sans coût WHEN création THEN refuse la ligne")
        void shouldRefuseLineWithoutCost() {
            assertThatThrownBy(() -> new ImportContracts.Line(clientId, null, null, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Import accepté")
    class Accepted {

        @Test
        @DisplayName("GIVEN des lignes valides WHEN execute THEN crée tous les contrats dans l'ordre des lignes")
        void shouldCreateEveryContractInLinesOrder() {
            final UUID otherClientId = UUID.randomUUID();
            final ImportContracts.Command command = commandOf(
                    new ImportContracts.Line(clientId, now, now.plusYears(1), VALID_COST),
                    new ImportContracts.Line(otherClientId, null, null, new BigDecimal("2000.00"))
            );
            when(clientRepository.findExistingIds(Set.of(clientId, otherClientId)))
                    .thenReturn(Set.of(clientId, otherClientId));
            when(contractRepository.insertAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            final List<Contract> created = handler.execute(command);

            assertThat(created)
                    .extracting(Contract::clientId, contract -> contract.costAmount().value())
                    .containsExactly(
                            tuple(clientId, VALID_COST),
                            tuple(otherClientId, new BigDecimal("2000.00")));
        }

        @Test
        @DisplayName("GIVEN des lignes sans date de début WHEN execute THEN tous les contrats reçoivent le même instant")
        void shouldUseSingleNowForAllContracts() {
            when(clientRepository.findExistingIds(Set.of(clientId))).thenReturn(Set.of(clientId));

            handler.execute(commandOf(validLine(), validLine()));

            verify(contractRepository).insertAll(insertedContracts.capture());
            assertThat(insertedContracts.getValue()).hasSize(2).allSatisfy(contract -> {
                assertThat(contract.period().startDate()).isEqualTo(now);
                assertThat(contract.lastModified()).isEqualTo(now);
            });
        }
    }

    @Nested
    @DisplayName("Import rejeté")
    class Rejected {

        @Test
        @DisplayName("GIVEN une seule ligne invalide WHEN execute THEN n'insère aucun contrat")
        void shouldInsertNothingWhenOneLineIsInvalid() {
            when(clientRepository.findExistingIds(Set.of(clientId))).thenReturn(Set.of(clientId));
            final ImportContracts.Command command = commandOf(
                    validLine(),
                    new ImportContracts.Line(clientId, null, null, NEGATIVE_COST)
            );

            assertThatThrownBy(() -> handler.execute(command))
                    .isInstanceOf(ContractImportRejectedException.class);

            verify(contractRepository, never()).insertAll(anyList());
        }

        @Test
        @DisplayName("GIVEN plusieurs lignes invalides WHEN execute THEN rapporte chacune avec son index et sa raison")
        void shouldReportEveryRejectedLine() {
            final UUID unknownClientId = UUID.randomUUID();
            when(clientRepository.findExistingIds(Set.of(clientId, unknownClientId))).thenReturn(Set.of(clientId));
            final ImportContracts.Command command = commandOf(
                    new ImportContracts.Line(clientId, null, null, NEGATIVE_COST),
                    validLine(),
                    new ImportContracts.Line(clientId, now, now.minusDays(1), VALID_COST),
                    new ImportContracts.Line(unknownClientId, null, null, VALID_COST)
            );

            final ContractImportRejectedException rejection =
                    catchThrowableOfType(ContractImportRejectedException.class, () -> handler.execute(command));

            assertThat(rejection.rejections())
                    .extracting(ContractImportRejectedException.LineRejection::index,
                            ContractImportRejectedException.LineRejection::clientId,
                            ContractImportRejectedException.LineRejection::reason)
                    .containsExactly(
                            tuple(0, clientId, ContractImportRejectionReason.INVALID_COST),
                            tuple(2, clientId, ContractImportRejectionReason.INVALID_PERIOD),
                            tuple(3, unknownClientId, ContractImportRejectionReason.CLIENT_NOT_FOUND)
                    );
            assertThat(rejection.rejections()).allSatisfy(line -> assertThat(line.message()).isNotBlank());
            assertThat(rejection.rejections().getLast().message()).contains(unknownClientId.toString());
        }
    }
}
