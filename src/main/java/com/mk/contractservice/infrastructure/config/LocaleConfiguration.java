package com.mk.contractservice.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class LocaleConfiguration {

    @Bean
    public LocaleResolver localeResolver() {
        final AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.of("fr", "CH"));
        resolver.setSupportedLocales(List.of(
                Locale.of("fr", "CH"),  // Français suisse
                Locale.of("fr"),        // Français
                Locale.of("en"),        // Anglais
                Locale.of("de", "CH"),  // Allemand suisse
                Locale.of("de"),        // Allemand
                Locale.of("it", "CH"),  // Italien suisse
                Locale.of("it")         // Italien
        ));

        return resolver;
    }
}

