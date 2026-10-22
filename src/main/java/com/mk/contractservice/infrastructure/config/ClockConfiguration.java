package com.mk.contractservice.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.InstantSource;

@Configuration
public class ClockConfiguration {

    @Bean
    public InstantSource instantSource() {
        return InstantSource.system();
    }

    @Bean
    public Clock clock(final InstantSource instantSource, final TimeZoneProperties timeZoneProperties) {
        return instantSource.withZone(timeZoneProperties.timeZone());
    }
}
