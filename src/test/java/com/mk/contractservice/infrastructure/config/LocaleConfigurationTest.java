package com.mk.contractservice.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Locale Configuration Tests")
class LocaleConfigurationTest {

    @Autowired
    private LocaleResolver localeResolver;

    @Test
    @DisplayName("Should have LocaleResolver bean configured")
    void shouldHaveLocaleResolverBean() {
        assertThat(localeResolver).isNotNull();
    }

    @Test
    @DisplayName("Should have French Swiss (fr-CH) as default locale")
    void shouldHaveFrenchSwissAsDefaultLocale() {
        // Given: A mock request without Accept-Language header
        final var request = new org.springframework.mock.web.MockHttpServletRequest();

        // When: Resolving locale
        final Locale locale = localeResolver.resolveLocale(request);

        // Then: Default locale should be fr-CH
        assertThat(locale.getLanguage()).isEqualTo("fr");
        assertThat(locale.getCountry()).isEqualTo("CH");
        assertThat(locale.toLanguageTag()).isEqualTo("fr-CH");
    }

    @Test
    @DisplayName("Should support English locale via Accept-Language header")
    void shouldSupportEnglishLocale() {
        // Given: A request with Accept-Language: en
        final var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("Accept-Language", "en");

        // When: Resolving locale
        final Locale locale = localeResolver.resolveLocale(request);

        // Then: Locale should be English
        assertThat(locale.getLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("Should support German Swiss locale via Accept-Language header")
    void shouldSupportGermanSwissLocale() {
        // Given: A request with Accept-Language: de-CH
        final var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("Accept-Language", "de-CH");

        // When: Resolving locale
        final Locale locale = localeResolver.resolveLocale(request);

        // Then: Locale should be German Swiss
        assertThat(locale.getLanguage()).isEqualTo("de");
        assertThat(locale.getCountry()).isEqualTo("CH");
    }

    @Test
    @DisplayName("Should support Italian Swiss locale via Accept-Language header")
    void shouldSupportItalianSwissLocale() {
        // Given: A request with Accept-Language: it-CH
        final var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("Accept-Language", "it-CH");

        // When: Resolving locale
        final Locale locale = localeResolver.resolveLocale(request);

        // Then: Locale should be Italian Swiss
        assertThat(locale.getLanguage()).isEqualTo("it");
        assertThat(locale.getCountry()).isEqualTo("CH");
    }

    @Test
    @DisplayName("Should fallback to fr-CH for unsupported locale")
    void shouldFallbackToDefaultForUnsupportedLocale() {
        // Given: A request with unsupported locale
        final var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("Accept-Language", "zh-CN"); // Chinese not supported

        // When: Resolving locale
        final Locale locale = localeResolver.resolveLocale(request);

        // Then: Should fallback to default fr-CH
        assertThat(locale.getLanguage()).isEqualTo("fr");
        assertThat(locale.getCountry()).isEqualTo("CH");
    }
}

