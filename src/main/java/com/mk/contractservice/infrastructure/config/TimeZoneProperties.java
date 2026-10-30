package com.mk.contractservice.infrastructure.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

@Validated
@ConfigurationProperties(prefix = "app")
public record TimeZoneProperties(@NotNull ZoneId timeZone) {
}
