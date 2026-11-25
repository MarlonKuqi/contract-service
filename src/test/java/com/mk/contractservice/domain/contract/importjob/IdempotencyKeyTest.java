package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.exception.InvalidIdempotencyKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IdempotencyKey")
class IdempotencyKeyTest {

    @Test
    @DisplayName("GIVEN no value WHEN of THEN it is rejected as an invalid idempotency key")
    void shouldRejectNullValue() {
        assertThatThrownBy(() -> IdempotencyKey.of(null)).isInstanceOf(InvalidIdempotencyKeyException.class);
    }
}
