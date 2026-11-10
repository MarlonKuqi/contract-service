package com.mk.contractservice.acceptance;

import com.mk.contractservice.ContractServiceApplication;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState;
import com.mk.contractservice.features.contract.importjob.SubmitContractImportJob;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisplayName("Technical guarantee: an instance that shuts down hands its import job over without losing a line")
class ContractImportJobShutdownAcceptanceTest {

    private static final int LINES = 100_000;

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.0-alpine");

    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void startDatabase() {
        POSTGRES.start();
        jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()));
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    private static ConfigurableApplicationContext startInstance() {
        return new SpringApplicationBuilder(ContractServiceApplication.class)
                .run(
                        "--spring.profiles.active=test",
                        "--server.port=0",
                        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword(),
                        "--app.contract-import.worker-enabled=true");
    }

    private static UUID createClient(final ConfigurableApplicationContext instance) {
        return instance.getBean(ClientRepository.class)
                .save(ClientFixtures.newCompany("Shutdown Test",
                        TestDataHelper.uniqueEmail("shutdown.test"),
                        TestDataHelper.randomSwissPhoneNumber(),
                        TestDataHelper.uniqueCompanyIdentifier("CHE")))
                .id();
    }

    private static <T> T jobValue(final UUID jobId, final String column, final Class<T> type) {
        return jdbcTemplate.queryForObject("select %s from contracts.contract_import_job where id = ?".formatted(
                column), type, jobId);
    }

    @Test
    @DisplayName(
        "GIVEN a job running on an instance WHEN the instance closes THEN the job goes back to pending and the next instance completes it")
    void shouldHandOverJobOnShutdown() {
        final UUID jobId;
        final UUID clientId;
        try (ConfigurableApplicationContext firstInstance = startInstance()) {
            clientId = createClient(firstInstance);
            final String file = "clientId;type;startDate;endDate;costAmount\n" + (clientId + ";LAA;;;100.00\n")
                    .repeat(LINES);
            jobId = firstInstance.getBean(SubmitContractImportJob.class)
                    .execute(new SubmitContractImportJob.Command("contracts.csv",
                            file.getBytes(StandardCharsets.UTF_8),
                            "counselor.test",
                            null))
                    .id();
            await().atMost(Duration.ofSeconds(60))
                    .pollInterval(Duration.ofMillis(20))
                    .until(() -> jobValue(jobId, "processed_lines", Integer.class) > 0);
        }

        assertThat(jobValue(jobId, "status", String.class)).isEqualTo(ContractImportJobState.Status.PENDING.name());
        assertThat(jobValue(jobId, "processed_lines", Integer.class)).isLessThan(LINES);
        assertThat(jobValue(jobId, "lease_until", java.time.OffsetDateTime.class)).isNull();

        try (var _ = startInstance()) {
            await().atMost(Duration.ofMinutes(2))
                    .pollInterval(Duration.ofMillis(200))
                    .until(() -> ContractImportJobState.Status.COMPLETED.name()
                            .equals(jobValue(jobId, "status", String.class)));
        }

        assertThat(jobValue(jobId, "imported_count", Integer.class)).isEqualTo(LINES);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from contracts.contract where client_id = ?",
                Long.class,
                clientId)).isEqualTo(LINES);
    }
}
