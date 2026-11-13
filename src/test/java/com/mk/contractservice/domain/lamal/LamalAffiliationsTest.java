package com.mk.contractservice.domain.lamal;

import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFixtures;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("LamalAffiliations")
class LamalAffiliationsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final LocalDateTime Y2027 = LocalDateTime.of(2027, 1, 1, 0, 0);
    private static final LocalDateTime Y2028 = LocalDateTime.of(2028, 1, 1, 0, 0);
    private static final LocalDateTime Y2029 = LocalDateTime.of(2029, 1, 1, 0, 0);
    private static final LocalDateTime Y2030 = LocalDateTime.of(2030, 1, 1, 0, 0);
    private static final LocalDateTime MID_2027 = Y2027.plusMonths(6);
    private static final LocalDateTime MID_2028 = Y2028.plusMonths(6);
    private static final BigDecimal COST = new BigDecimal("300.00");
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID OTHER_PERSON_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    private static Contract existingLamal(final UUID personId, final LocalDateTime start, final LocalDateTime end) {
        return ContractFixtures.existingContract(UUID.randomUUID(), personId, start, end, COST, NOW);
    }

    private static Contract existingLamal(final LocalDateTime start, final LocalDateTime end) {
        return existingLamal(PERSON_ID, start, end);
    }

    private static Contract newLamal(final LocalDateTime start, final LocalDateTime end) {
        return ContractFixtures.newContract(PERSON_ID, start, end, COST, NOW);
    }

    private static Contract newCompanyContract(final ContractType type) {
        return ContractFixtures.newContract(ClientFixtures.existingCompany(COMPANY_ID), type, Y2027, null, COST, NOW);
    }

    private static LamalAffiliations affiliationsWith(final Contract... terms) {
        return LamalAffiliations.of(List.of(
                LamalAffiliationFixtures.loadedFor(List.of(PERSON_ID), List.of(terms)).getFirst(),
                LamalAffiliation.reconstituteFromDatabase(OTHER_PERSON_ID, List.of())));
    }

    @Nested
    @DisplayName("reconstituteFromDatabase")
    class ReconstituteFromDatabase {

        @Test
        @DisplayName("GIVEN no person id WHEN reconstituting an affiliation THEN it is refused")
        void shouldRefuseNullPersonId() {
            assertThatThrownBy(() -> LamalAffiliation.reconstituteFromDatabase(null, List.of()))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }

        @Test
        @DisplayName("GIVEN no terms WHEN reconstituting an affiliation THEN it is refused")
        void shouldRefuseNullTerms() {
            assertThatThrownBy(() -> LamalAffiliation.reconstituteFromDatabase(PERSON_ID, null))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }

        @Test
        @DisplayName("GIVEN a stored term without contract id WHEN reconstituting it THEN it is refused")
        void shouldRefuseTermWithoutContractId() {
            ContractPeriod period = ContractPeriod.reconstituteFromDatabase(Y2027, null);

            assertThatThrownBy(() -> LamalTerm.reconstituteFromDatabase(null, period))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }

        @Test
        @DisplayName("GIVEN a stored term without period WHEN reconstituting it THEN it is refused")
        void shouldRefuseTermWithoutPeriod() {
            assertThatThrownBy(() -> LamalTerm.reconstituteFromDatabase(UUID.randomUUID(), null))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }
    }

    @Nested
    @DisplayName("admit")
    class Admit {

        static Stream<Arguments> relativePositions() {
            return Stream.of(
                    arguments("new one entirely before, with a gap", Y2028, Y2029, Y2027, MID_2027, false),
                    arguments("new one ends when the existing one starts", Y2028, Y2029, Y2027, Y2028, false),
                    arguments("new one ends during the existing one", Y2028, Y2029, Y2027, MID_2028, true),
                    arguments("same start, new one ends first", Y2028, Y2030, Y2028, Y2029, true),
                    arguments("new one strictly inside the existing one", Y2027, Y2030, Y2028, Y2029, true),
                    arguments("same end, new one starts later", Y2027, Y2029, Y2028, Y2029, true),
                    arguments("identical periods", Y2028, Y2029, Y2028, Y2029, true),
                    arguments("new one contains the existing one", Y2028, Y2029, Y2027, Y2030, true),
                    arguments("new one starts during the existing one", Y2027, Y2029, Y2028, Y2030, true),
                    arguments("new one starts when the existing one ends", Y2027, Y2028, Y2028, Y2029, false),
                    arguments("new one entirely after, with a gap", Y2027, Y2028, Y2029, Y2030, false),
                    arguments("open-ended existing one, new one ends when it starts", Y2028, null, Y2027, Y2028, false),
                    arguments("open-ended existing one, new one later", Y2028, null, Y2029, Y2030, true),
                    arguments("both open-ended", Y2028, null, Y2027, null, true),
                    arguments("open-ended new one starting before the existing one", Y2028, Y2029, Y2027, null, true),
                    arguments("open-ended new one starting when the existing one ends",
                            Y2027,
                            Y2028,
                            Y2028,
                            null,
                            false)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("relativePositions")
        @DisplayName("GIVEN an active LAMal WHEN admitting a LAMal at any relative position THEN only an overlap is refused")
        void shouldRefuseOnlyOverlappingPositions(final String position,
                final LocalDateTime existingStart,
                final LocalDateTime existingEnd,
                final LocalDateTime newStart,
                final LocalDateTime newEnd,
                final boolean overlaps) {
            LamalAffiliations affiliations = affiliationsWith(existingLamal(existingStart, existingEnd));
            Contract candidate = newLamal(newStart, newEnd);

            if (overlaps) {
                assertThatThrownBy(() -> affiliations.admit(candidate, NOW))
                        .isInstanceOf(OverlappingContractException.class);
            } else {
                assertThatCode(() -> affiliations.admit(candidate, NOW)).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("GIVEN a LAMal in 2027 WHEN admitting a LAMal starting during it THEN the refusal names the person, the type and the covered period")
        void shouldDescribeTheOverlappedPeriod() {
            LamalAffiliations affiliations = affiliationsWith(existingLamal(Y2027, Y2028));
            Contract overlapping = newLamal(MID_2027, Y2029);

            assertThatThrownBy(() -> affiliations.admit(overlapping, NOW))
                    .isInstanceOf(OverlappingContractException.class)
                    .hasMessageContaining(PERSON_ID.toString())
                    .hasMessageContaining("LAMAL")
                    .hasMessageContaining(Y2027.toString());
        }

        @Test
        @DisplayName("GIVEN an open-ended LAMal WHEN admitting a later LAMal THEN the refusal says the covered period has no end")
        void shouldDescribeAnOpenEndedPeriod() {
            LamalAffiliations affiliations = affiliationsWith(existingLamal(Y2027, null));

            assertThatThrownBy(() -> affiliations.admit(newLamal(Y2029, null), NOW))
                    .isInstanceOf(OverlappingContractException.class)
                    .hasMessageContaining("no end date");
        }

        @Test
        @DisplayName("GIVEN a LAMal ending one microsecond after now WHEN admitting a LAMal over its period THEN it is refused, the LAMal being still active")
        void shouldRefuseLamalOverlappingOneEndingJustAfterNow() {
            LocalDateTime start = NOW.minusMonths(10);
            LamalAffiliations affiliations = affiliationsWith(existingLamal(start, NOW.plus(1, ChronoUnit.MICROS)));

            assertThatThrownBy(() -> affiliations.admit(newLamal(start, null), NOW))
                    .isInstanceOf(OverlappingContractException.class);
        }

        @Test
        @DisplayName("GIVEN a LAMal that already ended WHEN admitting a LAMal over its period THEN it is accepted, only active LAMal counting")
        void shouldAcceptLamalOverlappingEndedOne() {
            LamalAffiliations affiliations = affiliationsWith(existingLamal(NOW.minusYears(2), NOW.minusYears(1)));
            Contract backdated = newLamal(NOW.minusYears(2).plusMonths(3), NOW.plusYears(1));

            assertThatCode(() -> affiliations.admit(backdated, NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a started LAMal closed now by a cancellation WHEN admitting the corrected LAMal from the same start THEN it is accepted")
        void shouldAcceptLamalReplacingOneClosedNow() {
            LocalDateTime start = NOW.minusMonths(10);
            LamalAffiliations affiliations = affiliationsWith(existingLamal(start, NOW));

            assertThatCode(() -> affiliations.admit(newLamal(start, null), NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a still active LAMal that started in the past WHEN admitting a LAMal over its past THEN it is refused")
        void shouldRefuseLamalOverlappingThePastOfAnActiveOne() {
            LamalAffiliations affiliations = affiliationsWith(existingLamal(NOW.minusYears(1), Y2027));
            Contract overlapping = newLamal(NOW.minusYears(2), NOW.minusMonths(6));

            assertThatThrownBy(() -> affiliations.admit(overlapping, NOW))
                    .isInstanceOf(OverlappingContractException.class);
        }

        @Test
        @DisplayName("GIVEN a future LAMal closed before it started WHEN admitting a LAMal over its period THEN it is accepted, the closed one covering nothing")
        void shouldIgnoreLamalClosedBeforeItStarted() {
            LamalAffiliations affiliations = affiliationsWith(existingLamal(Y2028, Y2027));

            assertThatCode(() -> affiliations.admit(newLamal(Y2027, null), NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a LAMal of another person WHEN admitting a LAMal over the same period THEN it is accepted")
        void shouldAcceptLamalOfAnotherPerson() {
            LamalAffiliations affiliations = LamalAffiliations.of(List.of(
                    LamalAffiliation.reconstituteFromDatabase(PERSON_ID, List.of()),
                    LamalAffiliationFixtures.loadedFor(List.of(OTHER_PERSON_ID),
                            List.of(existingLamal(OTHER_PERSON_ID, Y2027, null))).getFirst()));

            assertThatCode(() -> affiliations.admit(newLamal(Y2027, null), NOW)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = ContractType.class, names = {"LAA", "LPP"})
        @DisplayName("GIVEN a company without LAMal affiliation WHEN admitting several contracts of a type allowing overlaps THEN all are accepted")
        void shouldAcceptOverlappingContractsOfTypesAllowingIt(final ContractType type) {
            LamalAffiliations affiliations = affiliationsWith();

            assertThatCode(() -> affiliations.admit(newCompanyContract(type), NOW)).doesNotThrowAnyException();
            assertThatCode(() -> affiliations.admit(newCompanyContract(type), NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN an admitted LAMal WHEN admitting a LAMal overlapping it THEN the second one is refused")
        void shouldBlockWithAdmittedLamal() {
            LamalAffiliations affiliations = affiliationsWith();
            affiliations.admit(newLamal(Y2027, null), NOW);

            assertThatThrownBy(() -> affiliations.admit(newLamal(Y2028, Y2029), NOW))
                    .isInstanceOf(OverlappingContractException.class);
        }

        @Test
        @DisplayName("GIVEN a refused LAMal WHEN admitting a LAMal that only overlaps the refused one THEN it is accepted")
        void shouldNotBlockWithRefusedLamal() {
            LamalAffiliations affiliations = affiliationsWith(existingLamal(Y2027, Y2028));
            assertThatThrownBy(() -> affiliations.admit(newLamal(Y2027, Y2029), NOW))
                    .isInstanceOf(OverlappingContractException.class);

            assertThatCode(() -> affiliations.admit(newLamal(Y2028, Y2029), NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN an admitted LAMal already ended WHEN admitting a LAMal over its period THEN it is accepted, the ended one not blocking")
        void shouldNotBlockWithAdmittedLamalAlreadyEnded() {
            LamalAffiliations affiliations = affiliationsWith();
            affiliations.admit(newLamal(NOW.minusYears(2), NOW.minusYears(1)), NOW);

            assertThatCode(() -> affiliations.admit(newLamal(NOW.minusYears(2), null), NOW))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a stored LAMal WHEN admitting its own new period over the previous one THEN it is accepted, a contract not overlapping itself")
        void shouldNotCompareTheContractWithItself() {
            Contract stored = existingLamal(Y2027, Y2028);
            LamalAffiliations affiliations = affiliationsWith(stored);
            Contract extended = stored.changePeriod(ContractPeriod.of(Y2027, Y2029, NOW),
                    ClientFixtures.existingPerson(PERSON_ID).contractHolder(),
                    NOW);

            assertThatCode(() -> affiliations.admit(extended, NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN a stored LAMal moved to a new period WHEN admitting a LAMal over its previous period THEN it is accepted, the previous period being replaced")
        void shouldReplaceThePreviousPeriodOfAMovedContract() {
            Contract stored = existingLamal(Y2027, Y2028);
            LamalAffiliations affiliations = affiliationsWith(stored);
            affiliations.admit(stored.changePeriod(ContractPeriod.of(Y2029, Y2030, NOW),
                    ClientFixtures.existingPerson(PERSON_ID).contractHolder(),
                    NOW), NOW);

            assertThatCode(() -> affiliations.admit(newLamal(Y2027, Y2028), NOW)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN the affiliation of a person WHEN admitting a LAMal of another person THEN it is refused")
        void shouldRefuseTermOfAnotherPerson() {
            LamalAffiliation affiliation = LamalAffiliation.reconstituteFromDatabase(PERSON_ID, List.of());
            Contract lamalOfAnotherPerson = existingLamal(OTHER_PERSON_ID, Y2027, null);

            assertThatThrownBy(() -> affiliation.admit(lamalOfAnotherPerson, NOW))
                    .isInstanceOf(InvalidDomainObjectError.class);
        }

        @Test
        @DisplayName("GIVEN the affiliation of a person WHEN admitting a contract other than a LAMal THEN it is refused, only LAMal being its terms")
        void shouldRefuseTermOfAnotherType() {
            LamalAffiliation affiliation = LamalAffiliation.reconstituteFromDatabase(PERSON_ID, List.of());
            Contract laa = ContractFixtures.existingContract(UUID.randomUUID(),
                    PERSON_ID,
                    ContractType.LAA,
                    Y2027,
                    null,
                    COST,
                    NOW);

            assertThatThrownBy(() -> affiliation.admit(laa, NOW)).isInstanceOf(InvalidDomainObjectError.class);
        }

        @Test
        @DisplayName("GIVEN no affiliation loaded for the person WHEN admitting a LAMal THEN it fails, the affiliation being required to check the rule")
        void shouldFailWithoutAffiliationOfThePerson() {
            LamalAffiliations affiliations = LamalAffiliations.of(List.of());

            assertThatThrownBy(() -> affiliations.admit(newLamal(Y2027, null), NOW))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
