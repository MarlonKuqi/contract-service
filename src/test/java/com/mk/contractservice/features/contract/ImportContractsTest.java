package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFixtures;
import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.LamalFranchise;
import com.mk.contractservice.domain.contract.exception.ContractImportRejectedException;
import com.mk.contractservice.domain.contract.exception.InvalidImportSizeException;
import com.mk.contractservice.domain.lamal.LamalAffiliationFixtures;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
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

    @Mock
    private LamalAffiliationRepository lamalAffiliationRepository;

    @Captor
    private ArgumentCaptor<List<Contract>> insertedContracts;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-15T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    private final LocalDateTime now = LocalDateTime.now(clock);
    private final UUID clientId = UUID.randomUUID();

    private ImportContracts.Handler handler;

    @BeforeEach
    void setUp() {
        lenient().when(clientRepository.lockAgainstDeletion(anySet()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(lamalAffiliationRepository.loadForAdmission(anySet()))
                .thenAnswer(invocation -> LamalAffiliationFixtures.loadedFor(invocation.getArgument(0), List.of()));
        handler = new ImportContracts.Handler(clientRepository,
                contractRepository,
                lamalAffiliationRepository,
                clock);
    }

    private ImportContracts.Line validLine() {
        return new ImportContracts.Line(clientId, ContractType.LAMAL, 300, null, null, VALID_COST);
    }

    private ImportContracts.Line laaLine() {
        return new ImportContracts.Line(clientId, ContractType.LAA, null, null, null, VALID_COST);
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
            assertThatThrownBy(() -> new ImportContracts.Line(null, ContractType.LAMAL, 300, null, null, VALID_COST))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("GIVEN une ligne sans type de contrat WHEN création THEN refuse la ligne")
        void shouldRefuseLineWithoutType() {
            assertThatThrownBy(() -> new ImportContracts.Line(clientId, null, null, null, null, VALID_COST))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("GIVEN une ligne sans coût WHEN création THEN refuse la ligne")
        void shouldRefuseLineWithoutCost() {
            assertThatThrownBy(() -> new ImportContracts.Line(clientId, ContractType.LAMAL, 300, null, null, null))
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
                    new ImportContracts.Line(clientId, ContractType.LAMAL, 300, now, now.plusYears(1), VALID_COST),
                    new ImportContracts.Line(otherClientId,
                            ContractType.LAMAL,
                            1500,
                            null,
                            null,
                            new BigDecimal("2000.00"))
            );
            when(clientRepository.findAllByIds(Set.of(clientId, otherClientId)))
                    .thenReturn(List.of(ClientFixtures.existingPerson(clientId),
                            ClientFixtures.existingPerson(otherClientId)));
            when(contractRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            final List<Contract> created = handler.execute(command);

            verify(contractRepository).saveAll(created);
            assertThat(created)
                    .extracting(Contract::clientId,
                            contract -> contract.franchise().map(LamalFranchise::value).orElseThrow(),
                            contract -> contract.costAmount().value())
                    .containsExactly(
                            tuple(clientId, 300, VALID_COST),
                            tuple(otherClientId, 1500, new BigDecimal("2000.00")));
        }

        @Test
        @DisplayName("GIVEN des lignes sans date de début WHEN execute THEN tous les contrats reçoivent le même instant")
        void shouldUseSingleNowForAllContracts() {
            when(clientRepository.findAllByIds(Set.of(clientId)))
                    .thenReturn(List.of(ClientFixtures.existingCompany(clientId)));

            handler.execute(commandOf(laaLine(), laaLine()));

            verify(contractRepository).saveAll(insertedContracts.capture());
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
            when(clientRepository.findAllByIds(Set.of(clientId)))
                    .thenReturn(List.of(ClientFixtures.existingPerson(clientId)));
            final ImportContracts.Command command = commandOf(
                    validLine(),
                    new ImportContracts.Line(clientId, ContractType.LAMAL, 300, null, null, NEGATIVE_COST)
            );

            assertThatThrownBy(() -> handler.execute(command))
                    .isInstanceOf(ContractImportRejectedException.class);
            verify(contractRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("GIVEN un client supprimé pendant que l'import attendait le verrou WHEN execute THEN rejette sa ligne comme client introuvable")
        void shouldRejectLineOfClientDeletedWhileWaitingForTheLock() {
            final UUID deletedClientId = UUID.randomUUID();
            when(clientRepository.lockAgainstDeletion(Set.of(clientId, deletedClientId))).thenReturn(Set.of(clientId));
            when(clientRepository.findAllByIds(Set.of(clientId)))
                    .thenReturn(List.of(ClientFixtures.existingPerson(clientId)));
            lenient().when(clientRepository.findAllByIds(Set.of(clientId, deletedClientId)))
                    .thenReturn(List.of(ClientFixtures.existingPerson(clientId),
                            ClientFixtures.existingPerson(deletedClientId)));
            final ImportContracts.Command command = commandOf(
                    validLine(),
                    new ImportContracts.Line(deletedClientId, ContractType.LAMAL, 300, null, null, VALID_COST)
            );

            final ContractImportRejectedException rejection = catchThrowableOfType(
                    ContractImportRejectedException.class,
                    () -> handler.execute(command));

            assertThat(rejection.rejections())
                    .extracting(ContractImportRejectedException.LineRejection::index,
                            ContractImportRejectedException.LineRejection::clientId,
                            ContractImportRejectedException.LineRejection::reason)
                    .containsExactly(tuple(1, deletedClientId, ContractImportRejectionReason.CLIENT_NOT_FOUND));
            verify(contractRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("GIVEN plusieurs lignes invalides dont une LPP pour une personne WHEN execute THEN rapporte chacune avec son index et sa raison")
        void shouldReportEveryRejectedLine() {
            final UUID unknownClientId = UUID.randomUUID();
            when(clientRepository.findAllByIds(Set.of(clientId, unknownClientId)))
                    .thenReturn(List.of(ClientFixtures.existingPerson(clientId)));
            final ImportContracts.Command command = commandOf(
                    new ImportContracts.Line(clientId, ContractType.LAMAL, 300, null, null, NEGATIVE_COST),
                    validLine(),
                    new ImportContracts.Line(clientId, ContractType.LAMAL, 300, now, now.minusDays(1), VALID_COST),
                    new ImportContracts.Line(unknownClientId, ContractType.LAMAL, 300, null, null, VALID_COST),
                    new ImportContracts.Line(clientId, ContractType.LPP, null, null, null, VALID_COST)
            );

            final ContractImportRejectedException rejection = catchThrowableOfType(
                    ContractImportRejectedException.class,
                    () -> handler.execute(command));

            assertThat(rejection.rejections())
                    .extracting(ContractImportRejectedException.LineRejection::index,
                            ContractImportRejectedException.LineRejection::clientId,
                            ContractImportRejectedException.LineRejection::reason)
                    .containsExactly(
                            tuple(0, clientId, ContractImportRejectionReason.INVALID_COST),
                            tuple(2, clientId, ContractImportRejectionReason.INVALID_PERIOD),
                            tuple(3, unknownClientId, ContractImportRejectionReason.CLIENT_NOT_FOUND),
                            tuple(4, clientId, ContractImportRejectionReason.TYPE_NOT_ALLOWED)
                    );
            assertThat(rejection.rejections()).allSatisfy(line -> assertThat(line.message()).isNotBlank());
            assertThat(rejection.rejections().get(2).message()).contains(unknownClientId.toString());
            verify(contractRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("GIVEN une personne avec une LAMal sans fin WHEN une ligne lui ajoute une LAMal THEN rejette la ligne pour chevauchement")
        void shouldRejectLineOverlappingExistingContract() {
            when(clientRepository.findAllByIds(Set.of(clientId)))
                    .thenReturn(List.of(ClientFixtures.existingPerson(clientId)));
            when(lamalAffiliationRepository.loadForAdmission(Set.of(clientId)))
                    .thenReturn(LamalAffiliationFixtures.loadedFor(Set.of(clientId),
                            List.of(ContractFixtures.existingContract(UUID.randomUUID(),
                                    clientId,
                                    now.minusYears(1),
                                    null,
                                    VALID_COST,
                                    now))));
            final ImportContracts.Command command = commandOf(validLine());

            final ContractImportRejectedException rejection = catchThrowableOfType(
                    ContractImportRejectedException.class,
                    () -> handler.execute(command));

            assertThat(rejection.rejections())
                    .extracting(ContractImportRejectedException.LineRejection::index,
                            ContractImportRejectedException.LineRejection::reason)
                    .containsExactly(tuple(0, ContractImportRejectionReason.OVERLAPPING_CONTRACT));
            verify(contractRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("GIVEN deux lignes LAMal qui se chevauchent pour une personne WHEN execute THEN rejette la seconde")
        void shouldRejectSecondOfTwoOverlappingLines() {
            when(clientRepository.findAllByIds(Set.of(clientId)))
                    .thenReturn(List.of(ClientFixtures.existingPerson(clientId)));
            final ImportContracts.Command command = commandOf(
                    new ImportContracts.Line(clientId, ContractType.LAMAL, 300, now, now.plusYears(1), VALID_COST),
                    new ImportContracts.Line(clientId, ContractType.LAMAL, 300, now.plusYears(1), null, VALID_COST),
                    new ImportContracts.Line(clientId, ContractType.LAMAL, 300, now.plusMonths(6), null, VALID_COST)
            );

            final ContractImportRejectedException rejection = catchThrowableOfType(
                    ContractImportRejectedException.class,
                    () -> handler.execute(command));

            assertThat(rejection.rejections())
                    .extracting(ContractImportRejectedException.LineRejection::index,
                            ContractImportRejectedException.LineRejection::reason)
                    .containsExactly(tuple(2, ContractImportRejectionReason.OVERLAPPING_CONTRACT));
            verify(contractRepository, never()).saveAll(anyList());
        }
    }
}
