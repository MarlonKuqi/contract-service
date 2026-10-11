package com.mk.contractservice.acceptance.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
@Import(TestSecurityConfig.class)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18.0-alpine")
                .withDatabaseName("contract_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
    }
}

