package com.mk.contractservice.domain.contract.factory;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.shared.exception.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContractFactory")
class ContractFactoryTest {

    @Nested
    @DisplayName("Création de contrat")
    class CreateContractTest {

        @Test
        @DisplayName("GIVEN paramètres valides WHEN create THEN retourne un contrat avec les bonnes valeurs")
        void shouldCreateContractWithCorrectValues() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(12);
            BigDecimal costAmount = new BigDecimal("1500.00");

            // When
            Contract contract = ContractFactory.create(clientId, startDate, endDate, costAmount);

            // Then
            assertThat(contract).isNotNull();
            assertThat(contract.getClientId()).isEqualTo(clientId);
            assertThat(contract.getPeriod().getStartDate()).isEqualTo(startDate);
            assertThat(contract.getPeriod().getEndDate()).isEqualTo(endDate);
            assertThat(contract.getCostAmount().getValue()).isEqualByComparingTo(costAmount);
        }

        @Test
        @DisplayName("GIVEN plusieurs appels WHEN create THEN génère des IDs clients uniques")
        void shouldGenerateUniqueIds() {
            // Given
            UUID clientId1 = UUID.randomUUID();
            UUID clientId2 = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(12);
            BigDecimal costAmount = new BigDecimal("1500.00");

            // When
            Contract contract1 = ContractFactory.create(clientId1, startDate, endDate, costAmount);
            Contract contract2 = ContractFactory.create(clientId2, startDate, endDate, costAmount);

            // Then
            assertThat(contract1.getClientId()).isNotEqualTo(contract2.getClientId());
        }
    }

    @Nested
    @DisplayName("Validation des paramètres")
    class ValidationErrors {

        @Test
        @DisplayName("GIVEN clientId null WHEN create THEN lève une exception")
        void shouldRejectNullClientId() {
            // Given
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(12);
            BigDecimal costAmount = new BigDecimal("1500.00");

            // When & Then
            assertThatThrownBy(() -> ContractFactory.create(null, startDate, endDate, costAmount))
                    .isInstanceOf(DomainValidationException.class);
        }


        @Test
        @DisplayName("GIVEN endDate avant startDate WHEN create THEN lève une exception")
        void shouldRejectEndDateBeforeStartDate() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.minusDays(1);
            BigDecimal costAmount = new BigDecimal("1500.00");

            // When & Then
            assertThatThrownBy(() -> ContractFactory.create(clientId, startDate, endDate, costAmount))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("GIVEN costAmount null WHEN create THEN lève une exception")
        void shouldRejectNullCostAmount() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(12);

            // When & Then
            assertThatThrownBy(() -> ContractFactory.create(clientId, startDate, endDate, null))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("GIVEN costAmount négatif WHEN create THEN lève une exception")
        void shouldRejectNegativeCostAmount() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(12);
            BigDecimal costAmount = new BigDecimal("-100.00");

            // When & Then
            assertThatThrownBy(() -> ContractFactory.create(clientId, startDate, endDate, costAmount))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("GIVEN costAmount zéro WHEN create THEN lève une exception")
        void shouldRejectZeroCostAmount() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(12);
            BigDecimal costAmount = BigDecimal.ZERO;

            // When & Then
            assertThatThrownBy(() -> ContractFactory.create(clientId, startDate, endDate, costAmount))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("GIVEN costAmount avec trop de décimales WHEN create THEN lève une exception")
        void shouldRejectCostAmountWithTooManyDecimals() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(12);
            BigDecimal costAmount = new BigDecimal("1500.123");

            // When & Then
            assertThatThrownBy(() -> ContractFactory.create(clientId, startDate, endDate, costAmount))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("GIVEN montant minimal WHEN create THEN crée le contrat")
        void shouldCreateContractWithMinimumCost() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusMonths(1);
            BigDecimal costAmount = new BigDecimal("0.01");

            // When
            Contract contract = ContractFactory.create(clientId, startDate, endDate, costAmount);

            // Then
            assertThat(contract).isNotNull();
            assertThat(contract.getCostAmount().getValue()).isEqualByComparingTo(costAmount);
        }

        @Test
        @DisplayName("GIVEN montant élevé WHEN create THEN crée le contrat")
        void shouldCreateContractWithLargeCost() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusYears(5);
            BigDecimal costAmount = new BigDecimal("999999.99");

            // When
            Contract contract = ContractFactory.create(clientId, startDate, endDate, costAmount);

            // Then
            assertThat(contract).isNotNull();
            assertThat(contract.getCostAmount().getValue()).isEqualByComparingTo(costAmount);
        }

        @Test
        @DisplayName("GIVEN période courte WHEN create THEN crée le contrat")
        void shouldCreateContractWithShortPeriod() {
            // Given
            UUID clientId = UUID.randomUUID();
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusDays(1);
            BigDecimal costAmount = new BigDecimal("100.00");

            // When
            Contract contract = ContractFactory.create(clientId, startDate, endDate, costAmount);

            // Then
            assertThat(contract).isNotNull();
            assertThat(contract.getPeriod().getStartDate()).isEqualTo(startDate);
            assertThat(contract.getPeriod().getEndDate()).isEqualTo(endDate);
        }
    }
}

