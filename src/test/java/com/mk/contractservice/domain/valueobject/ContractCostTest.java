package com.mk.contractservice.domain.valueobject;

import com.mk.contractservice.domain.exception.InvalidContractCostException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContractCost - Business Rules Tests")
class ContractCostTest {

    @Test
    @DisplayName("GIVEN valid cost WHEN of() THEN create ContractCost")
    void shouldCreateWithValidCost() {
        ContractCost cost = ContractCost.of(new BigDecimal("100.50"));

        assertThat(cost.value()).isEqualByComparingTo("100.50");
    }

    @Test
    @DisplayName("GIVEN null cost WHEN of() THEN throw exception")
    void shouldRejectNull() {
        assertThatThrownBy(() -> ContractCost.of(null))
                .isInstanceOf(InvalidContractCostException.class)
                .hasMessageContaining("Contract cost amount must not be null");
    }

    @Test
    @DisplayName("GIVEN negative cost WHEN of() THEN throw exception")
    void shouldRejectNegative() {
        assertThatThrownBy(() -> ContractCost.of(new BigDecimal("-10.00")))
                .isInstanceOf(InvalidContractCostException.class)
                .hasMessageContaining("must be greater than zero");
    }

    @Test
    @DisplayName("GIVEN zero cost WHEN of() THEN throw exception")
    void shouldRejectZero() {
        assertThatThrownBy(() -> ContractCost.of(BigDecimal.ZERO))
                .isInstanceOf(InvalidContractCostException.class)
                .hasMessageContaining("must be greater than zero");
    }

    @Test
    @DisplayName("GIVEN cost with 3 decimal places WHEN of() THEN throw exception")
    void shouldRejectTooManyDecimals() {
        assertThatThrownBy(() -> ContractCost.of(new BigDecimal("100.123")))
                .isInstanceOf(InvalidContractCostException.class)
                .hasMessageContaining("Contract cost amount must have at most 2 decimal places");
    }

    @Test
    @DisplayName("GIVEN cost with 2 decimal places WHEN of() THEN accept")
    void shouldAcceptTwoDecimals() {
        ContractCost cost = ContractCost.of(new BigDecimal("100.99"));

        assertThat(cost.value()).isEqualByComparingTo("100.99");
    }

    @Test
    @DisplayName("GIVEN cost with 1 decimal place WHEN of() THEN accept")
    void shouldAcceptOneDecimal() {
        ContractCost cost = ContractCost.of(new BigDecimal("100.5"));

        assertThat(cost.value()).isEqualByComparingTo("100.5");
    }

    @Test
    @DisplayName("GIVEN cost with no decimal places WHEN of() THEN accept")
    void shouldAcceptNoDecimals() {
        ContractCost cost = ContractCost.of(new BigDecimal("100"));

        assertThat(cost.value()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("GIVEN large cost WHEN of() THEN accept")
    void shouldAcceptLargeCost() {
        ContractCost cost = ContractCost.of(new BigDecimal("999999999.99"));

        assertThat(cost.value()).isEqualByComparingTo("999999999.99");
    }
}

