package com.mk.contractservice.domain.lamal;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFixtures;
import com.mk.contractservice.domain.lamal.exception.InvalidLamalExemptionPeriodException;
import com.mk.contractservice.domain.lamal.exception.LamalExemptionConflictException;
import com.mk.contractservice.domain.lamal.exception.LamalExemptionNotAllowedException;
import com.mk.contractservice.domain.lamal.exception.OngoingLamalExemptionException;
import com.mk.contractservice.domain.shared.ContractHolder;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import com.mk.contractservice.domain.shared.Residence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LAMal exemption of a cross-border worker")
class LamalExemptionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final LocalDate BIRTH_DATE = LocalDate.of(1985, 3, 20);
    private static final LocalDate JAN_2026 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_2027 = LocalDate.of(2027, 1, 1);
    private static final LocalDate JAN_2028 = LocalDate.of(2028, 1, 1);
    private static final ContractHolder FRONTALIER = ContractHolder.person(BIRTH_DATE, Residence.FR);
    private static final ContractHolder RESIDENT = ContractHolder.person(BIRTH_DATE, Residence.CH);
    private static final BigDecimal COST = new BigDecimal("300.00");
    private static final UUID PERSON_ID = UUID.randomUUID();

    private static LamalExemption exemption(final LocalDate start, final LocalDate end) {
        return LamalExemption.of(start, end, FRONTALIER);
    }

    private static Contract lamal(final LocalDate start, final LocalDate end) {
        return ContractFixtures.existingContract(UUID.randomUUID(),
                PERSON_ID,
                start.atStartOfDay(),
                end == null ? null : end.atStartOfDay(),
                COST,
                NOW);
    }

    private static Contract newLamal(final LocalDate start, final LocalDate end) {
        return ContractFixtures.newContract(PERSON_ID,
                start.atStartOfDay(),
                end == null ? null : end.atStartOfDay(),
                COST,
                NOW);
    }

    private static LamalAffiliation affiliation(final LamalExemption exemption, final Contract... lamals) {
        return LamalAffiliationFixtures.loadedFor(PERSON_ID, List.of(lamals), exemption);
    }

    @Nested
    @DisplayName("Granting an exemption")
    class Granting {

        @ParameterizedTest
        @EnumSource(value = Residence.class, names = "CH", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("GIVEN a person living in a bordering country WHEN exempting them THEN the exemption is granted")
        void shouldExemptCrossBorderWorker(Residence residence) {
            LamalExemption exemption = LamalExemption.of(JAN_2026, null, ContractHolder.person(BIRTH_DATE, residence));

            assertThat(exemption.startDate()).isEqualTo(JAN_2026);
            assertThat(exemption.endDate()).isNull();
        }

        @Test
        @DisplayName("GIVEN a person living in Switzerland WHEN exempting them THEN it is refused")
        void shouldRefuseResident() {
            assertThatThrownBy(() -> LamalExemption.of(JAN_2026, null, RESIDENT))
                    .isInstanceOf(LamalExemptionNotAllowedException.class)
                    .hasMessage("Only a person living in a bordering country can be exempted from LAMal");
        }

        @Test
        @DisplayName("GIVEN a company WHEN exempting it THEN it is refused")
        void shouldRefuseCompany() {
            assertThatThrownBy(() -> LamalExemption.of(JAN_2026, null, ContractHolder.company()))
                    .isInstanceOf(LamalExemptionNotAllowedException.class);
        }

        @Test
        @DisplayName("GIVEN no start date WHEN exempting THEN it is refused")
        void shouldRefuseMissingStart() {
            assertThatThrownBy(() -> LamalExemption.of(null, null, FRONTALIER))
                    .isInstanceOf(InvalidLamalExemptionPeriodException.class)
                    .hasMessage("A LAMal exemption must have a start date");
        }

        @Test
        @DisplayName("GIVEN an end date on the start date WHEN exempting THEN it is refused")
        void shouldRefuseEndNotAfterStart() {
            assertThatThrownBy(() -> LamalExemption.of(JAN_2027, JAN_2027, FRONTALIER))
                    .isInstanceOf(InvalidLamalExemptionPeriodException.class)
                    .hasMessage("A LAMal exemption must end after it starts. Start: 2027-01-01, End: 2027-01-01");
        }

        @Test
        @DisplayName("GIVEN an invalid period for a resident WHEN exempting THEN the period error wins")
        void shouldCheckPeriodBeforeEligibility() {
            assertThatThrownBy(() -> LamalExemption.of(JAN_2027, JAN_2026, RESIDENT))
                    .isInstanceOf(InvalidLamalExemptionPeriodException.class);
        }

        @Test
        @DisplayName("GIVEN a stored exemption without start date WHEN reconstituting it THEN it is refused")
        void shouldRefuseStoredExemptionWithoutStart() {
            assertThatThrownBy(() -> LamalExemption.reconstituteFromDatabase(null, JAN_2027))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }
    }

    @Nested
    @DisplayName("Recording an exemption in the affiliation")
    class Recording {

        @Test
        @DisplayName("GIVEN no LAMal WHEN recording an exemption THEN the affiliation holds it")
        void shouldRecordExemption() {
            LamalAffiliation affiliation = affiliation(null);
            LamalExemption exemption = exemption(JAN_2026, null);

            LamalAffiliation recorded = affiliation.recordExemption(exemption, NOW);

            assertThat(recorded.exemption()).contains(exemption);
            assertThat(affiliation.exemption()).isEmpty();
        }

        @Test
        @DisplayName("GIVEN an active LAMal WHEN recording an exemption over its period THEN it is refused")
        void shouldRefuseExemptionOverActiveLamal() {
            LamalAffiliation affiliation = affiliation(null, lamal(JAN_2026, JAN_2027));

            assertThatThrownBy(() -> affiliation.recordExemption(exemption(LocalDate.of(2026, 6, 1), null), NOW))
                    .isInstanceOf(LamalExemptionConflictException.class)
                    .hasMessage(
                            "Person " + PERSON_ID + " has a LAMal contract from 2026-01-01T00:00 to 2027-01-01T00:00 overlapping this exemption");
        }

        @Test
        @DisplayName("GIVEN an expired LAMal WHEN recording an exemption over its period THEN it is recorded")
        void shouldIgnoreExpiredLamal() {
            LamalAffiliation affiliation = affiliation(null, lamal(LocalDate.of(2025, 1, 1), JAN_2026));

            assertThatCode(() -> affiliation.recordExemption(exemption(LocalDate.of(2025, 6, 1), null), NOW))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a LAMal ending on the exemption start WHEN recording the exemption THEN it is recorded")
        void shouldAcceptExemptionStartingWhenLamalEnds() {
            LamalAffiliation affiliation = affiliation(null, lamal(JAN_2026, JAN_2027));

            assertThatCode(() -> affiliation.recordExemption(exemption(JAN_2027, null), NOW))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a recorded exemption WHEN recording the same one THEN the affiliation is unchanged")
        void shouldIgnoreSameExemption() {
            LamalAffiliation affiliation = affiliation(exemption(JAN_2026, null));

            assertThat(affiliation.recordExemption(exemption(JAN_2026, null), NOW)).isSameAs(affiliation);
        }

        @Test
        @DisplayName("GIVEN an ongoing exemption WHEN closing it THEN the exemption gets its end date")
        void shouldCloseExemption() {
            LamalAffiliation affiliation = affiliation(exemption(JAN_2026, null));

            LamalAffiliation closed = affiliation.recordExemption(exemption(JAN_2026, JAN_2027), NOW);

            assertThat(closed.exemption()).contains(exemption(JAN_2026, JAN_2027));
        }
    }

    @Nested
    @DisplayName("Admitting a LAMal")
    class Admitting {

        @Test
        @DisplayName("GIVEN an exemption WHEN checking a LAMal over its period THEN it is refused")
        void shouldRefuseLamalDuringExemption() {
            LamalAffiliation affiliation = affiliation(exemption(JAN_2026, JAN_2028));

            assertThatThrownBy(() -> affiliation.ensureAdmits(newLamal(JAN_2027, null), NOW))
                    .isInstanceOf(LamalExemptionConflictException.class)
                    .hasMessage(
                            "Person " + PERSON_ID + " is exempted from LAMal from 2026-01-01 to 2028-01-01, a LAMal contract cannot cover this period");
        }

        @Test
        @DisplayName("GIVEN a closed exemption WHEN checking a LAMal over its past period THEN it is refused")
        void shouldRefuseLamalOverPastExemption() {
            LamalAffiliation affiliation = affiliation(exemption(LocalDate.of(2025, 1, 1), JAN_2026));

            assertThatThrownBy(() -> affiliation.ensureAdmits(newLamal(LocalDate.of(2025, 6, 1), JAN_2027), NOW))
                    .isInstanceOf(LamalExemptionConflictException.class);
        }

        @Test
        @DisplayName("GIVEN an exemption WHEN checking a LAMal starting when it ends THEN it is accepted")
        void shouldAdmitLamalAfterExemption() {
            LamalAffiliation affiliation = affiliation(exemption(JAN_2026, JAN_2027));

            assertThatCode(() -> affiliation.ensureAdmits(newLamal(JAN_2027, null), NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN an exemption WHEN admitting a LAMal THEN the exemption is kept")
        void shouldKeepExemptionWhenAdmitting() {
            LamalExemption exemption = exemption(JAN_2026, JAN_2027);

            LamalAffiliation admitted = affiliation(exemption).admit(newLamal(JAN_2027, null), NOW);

            assertThat(admitted.exemption()).contains(exemption);
        }
    }

    @Nested
    @DisplayName("Accepting a change of holder")
    class AcceptingHolder {

        @Test
        @DisplayName("GIVEN an ongoing exemption WHEN the person moves to Switzerland THEN it is refused")
        void shouldRefuseMoveToSwitzerlandDuringExemption() {
            LamalAffiliation affiliation = affiliation(exemption(JAN_2026, null));

            assertThatThrownBy(() -> affiliation.ensureAcceptsHolder(RESIDENT, NOW))
                    .isInstanceOf(OngoingLamalExemptionException.class)
                    .hasMessage(
                            "Person " + PERSON_ID + " is exempted from LAMal from 2026-01-01 to no end date: close the exemption before moving to Switzerland");
        }

        @Test
        @DisplayName("GIVEN an exemption starting later WHEN the person moves to Switzerland THEN it is refused")
        void shouldRefuseMoveToSwitzerlandBeforeFutureExemption() {
            LamalAffiliation affiliation = affiliation(exemption(JAN_2027, null));

            assertThatThrownBy(() -> affiliation.ensureAcceptsHolder(RESIDENT, NOW))
                    .isInstanceOf(OngoingLamalExemptionException.class);
        }

        @Test
        @DisplayName("GIVEN a closed exemption WHEN the person moves to Switzerland THEN it is accepted")
        void shouldAcceptMoveToSwitzerlandAfterExemption() {
            LamalAffiliation affiliation = affiliation(exemption(JAN_2026, NOW.toLocalDate()));

            assertThatCode(() -> affiliation.ensureAcceptsHolder(RESIDENT, NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN an ongoing exemption WHEN the person moves to another bordering country THEN it is accepted")
        void shouldAcceptMoveToAnotherBorderingCountry() {
            LamalAffiliation affiliation = affiliation(exemption(JAN_2026, null));

            assertThatCode(() -> affiliation.ensureAcceptsHolder(ContractHolder.person(BIRTH_DATE, Residence.DE), NOW))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN no exemption WHEN the person moves to Switzerland THEN it is accepted")
        void shouldAcceptMoveWithoutExemption() {
            assertThatCode(() -> affiliation(null).ensureAcceptsHolder(RESIDENT, NOW)).doesNotThrowAnyException();
        }
    }
}
