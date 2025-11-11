package com.mk.contractservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.mk.contractservice.util.FunctionalUtils.applyIfPresent;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FunctionalUtils Tests")
class FunctionalUtilsTest {

    @Test
    @DisplayName("applyIfPresent should apply action when value is not null")
    void shouldApplyActionWhenValuePresent() {
        List<String> results = new ArrayList<>();
        String value = "test";

        boolean applied = applyIfPresent(value, results::add);

        assertThat(applied).isTrue();
        assertThat(results).containsExactly("test");
    }

    @Test
    @DisplayName("applyIfPresent should not apply action when value is null")
    void shouldNotApplyActionWhenValueNull() {
        List<String> results = new ArrayList<>();
        assertThat(applyIfPresent((String) null, results::add)).isFalse();
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("applyIfPresent should work with multiple applications using OR operator")
    void shouldWorkWithMultipleApplications() {
        List<String> results = new ArrayList<>();

        boolean hasChanges = applyIfPresent("first", results::add)
                           | applyIfPresent((String) null, results::add)
                           | applyIfPresent("third", results::add);

        assertThat(hasChanges).isTrue();
        assertThat(results).containsExactly("first", "third");
    }

    @Test
    @DisplayName("applyIfPresent should return false when all values are null")
    void shouldReturnFalseWhenAllValuesNull() {
        List<String> results = new ArrayList<>();

        boolean hasChanges = applyIfPresent((String) null, results::add)
                           | applyIfPresent((String) null, results::add)
                           | applyIfPresent((String) null, results::add);

        assertThat(hasChanges).isFalse();
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("applyIfPresent should work with method references")
    void shouldWorkWithMethodReferences() {
        StringBuilder sb = new StringBuilder();

        applyIfPresent("Hello", sb::append);
        applyIfPresent(" ", sb::append);
        applyIfPresent("World", sb::append);

        assertThat(sb.toString()).isEqualTo("Hello World");
    }

}
