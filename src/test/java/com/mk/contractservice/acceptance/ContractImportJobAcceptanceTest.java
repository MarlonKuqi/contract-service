package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.batch.ContractImportBatchConfiguration;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobStatus;
import com.mk.contractservice.features.contract.importjob.ProcessContractImportBatch;
import com.mk.contractservice.features.shared.ContractImportCsv.RawLine;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mk.contractservice.controllers.contract.shared.ContractEndpoints.CONTRACTS_BASE;
import static com.mk.contractservice.controllers.contract.shared.ContractEndpoints.CONTRACT_IMPORT_JOBS;
import static com.mk.contractservice.controllers.contract.shared.ContractEndpoints.CONTRACT_TOTAL;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.contract-import.worker-enabled=true"
)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Feature: A counselor imports a large CSV file of contracts asynchronously")
class ContractImportJobAcceptanceTest {

    private static final String HEADER = "clientId;startDate;endDate;costAmount\n";
    private static final String MULTIPART = "multipart/form-data";
    private static final String JOB_PATH = CONTRACT_IMPORT_JOBS + "/{jobId}";

    @LocalServerPort
    private int port;

    @Autowired
    private Clock clock;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @Autowired
    private ContractImportJobRepository jobRepository;

    @Autowired
    private ProcessContractImportBatch processBatch;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String clientId;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);
        cleanUp();
        clientId = createPerson();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from contracts.contract_import_job");
        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();
    }

    private String createPerson() {
        return given()
                .body("""
                        {
                            "type": "PERSON",
                            "name": "Marie Durand",
                            "email": "%s",
                            "phone": "%s",
                            "birthDate": "1985-03-20"
                        }
                        """.formatted(TestDataHelper.uniqueEmail("marie.durand"), TestDataHelper.randomSwissPhoneNumber()))
                .when().post(ClientEndpoints.CLIENTS_BASE)
                .then().statusCode(201)
                .extract().path("id");
    }

    private ValidatableResponse submit(final byte[] content) {
        return given()
                .contentType(MULTIPART)
                .multiPart("file", "contracts.csv", content)
                .when().post(CONTRACT_IMPORT_JOBS)
                .then();
    }

    private ValidatableResponse submit(final String content) {
        return submit(content.getBytes(StandardCharsets.UTF_8));
    }

    private String submitAccepted(final String content) {
        return submit(content).statusCode(202).extract().path("jobId");
    }

    private void awaitStatus(final String jobId, final ContractImportJobStatus status) {
        awaitStatus(jobId, status, Duration.ofSeconds(30));
    }

    private void awaitStatus(final String jobId, final ContractImportJobStatus status, final Duration timeout) {
        await().atMost(timeout).pollInterval(Duration.ofMillis(100)).until(() -> status.name().equals(
                given().when().get(JOB_PATH, jobId).then().statusCode(200).extract().path("status")));
    }

    private ValidatableResponse getJob(final String jobId) {
        return given().when().get(JOB_PATH, jobId).then().statusCode(200);
    }

    private long contractsOfClient() {
        return jdbcTemplate.queryForObject(
                "select count(*) from contracts.contract where client_id = ?::uuid", Long.class, clientId);
    }

    private ContractImportJob persistInOneTransaction(final Supplier<ContractImportJob> job) {
        return new TransactionTemplate(transactionManager).execute(status -> job.get());
    }

    @Test
    @DisplayName("GIVEN a valid and a rejected line WHEN the job runs THEN valid lines are imported and the others are reported")
    void shouldImportValidLinesAndReportOthers() {
        final String unknownClient = UUID.randomUUID().toString();
        final String jobId = given()
                .contentType(MULTIPART)
                .multiPart("file", "contracts.csv", (HEADER
                        + clientId + ";;;100.00\n"
                        + clientId + ";;;-1\n"
                        + "\n"
                        + unknownClient + ";;;100.00\n"
                        + "garbage\n"
                        + clientId + ";2026-01-01T00:00:00;2025-01-01T00:00:00;100.00\n").getBytes(StandardCharsets.UTF_8))
                .when().post(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(202)
                .header("Location", containsString(CONTRACT_IMPORT_JOBS + "/"))
                .body("jobId", notNullValue())
                .body("status", equalTo(ContractImportJobStatus.PENDING.name()))
                .body("totalLines", equalTo(5))
                .extract().path("jobId");

        awaitStatus(jobId, ContractImportJobStatus.COMPLETED);

        getJob(jobId)
                .body("fileName", equalTo("contracts.csv"))
                .body("processedLines", equalTo(5))
                .body("importedCount", equalTo(1))
                .body("rejectedCount", equalTo(4))
                .body("finishedAt", notNullValue());
        assertThat(contractsOfClient()).isEqualTo(1);

        given().when().get(JOB_PATH + "/rejections", jobId)
                .then().statusCode(200)
                .body("totalElements", equalTo(4))
                .body("content.lineNumber", equalTo(List.of(3, 5, 6, 7)))
                .body("content.code", equalTo(List.of("invalidContractCost", "clientNotFound", "invalidLineFormat", "invalidContractPeriod")))
                .body("content[0].clientId", equalTo(clientId))
                .body("content[1].clientId", equalTo(unknownClient))
                .body("content[2].clientId", equalTo(null));
    }

    @Test
    @DisplayName("GIVEN more lines than a batch WHEN the job runs THEN every line is imported")
    void shouldImportAcrossBatches() {
        final int lines = ContractImportBatchConfiguration.CHUNK_SIZE + 1;

        final String jobId = submitAccepted(HEADER + (clientId + ";;;100.00\n").repeat(lines));

        awaitStatus(jobId, ContractImportJobStatus.COMPLETED);
        getJob(jobId).body("importedCount", equalTo(lines));
        assertThat(contractsOfClient()).isEqualTo(lines);
    }

    @Test
    @DisplayName("GIVEN 10000 contracts for one client costing 1 to 10000 WHEN the job runs THEN all are active from now and their total is right")
    void shouldImportTenThousandContractsForOneClient() {
        final int lines = 10_000;
        final LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        final String contracts = IntStream.rangeClosed(1, lines)
                .mapToObj(cost -> clientId + ";" + now + ";;" + cost + "\n")
                .collect(Collectors.joining());

        final String jobId = submitAccepted(HEADER + contracts);

        awaitStatus(jobId, ContractImportJobStatus.COMPLETED);
        getJob(jobId)
                .body("processedLines", equalTo(lines))
                .body("importedCount", equalTo(lines))
                .body("rejectedCount", equalTo(0));

        given().when().get(CONTRACTS_BASE + CONTRACT_TOTAL + "?clientId={clientId}", clientId)
                .then().statusCode(200)
                .body(equalTo("50005000.00"));

        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from contracts.contract
                where client_id = ?::uuid and start_date = ? and end_date is null
                """, Long.class, clientId, now.atZone(clock.getZone()).toOffsetDateTime())).isEqualTo(lines);
        assertThat(jdbcTemplate.queryForObject("""
                select count(distinct cost_amount) from contracts.contract
                where client_id = ?::uuid and cost_amount between 1 and 10000
                """, Long.class, clientId)).isEqualTo(lines);
    }

    @Test
    @DisplayName("GIVEN 50000 contracts shared by 100 clients WHEN the job runs THEN all are active from now and each client has the right total")
    void shouldImportFiftyThousandContractsForAHundredClients() {
        final int lines = 50_000;
        final int clientCount = 100;
        final LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        final List<String> clients = IntStream.range(0, clientCount).mapToObj(index -> createPerson()).toList();
        final String contracts = IntStream.rangeClosed(1, lines)
                .mapToObj(cost -> clients.get(cost % clientCount) + ";" + now + ";;" + cost + "\n")
                .collect(Collectors.joining());

        final String jobId = submitAccepted(HEADER + contracts);

        awaitStatus(jobId, ContractImportJobStatus.COMPLETED, Duration.ofMinutes(3));
        getJob(jobId)
                .body("processedLines", equalTo(lines))
                .body("importedCount", equalTo(lines))
                .body("rejectedCount", equalTo(0));

        for (int index = 0; index < clientCount; index++) {
            final int clientIndex = index;
            final long expectedTotal = IntStream.rangeClosed(1, lines)
                    .filter(cost -> cost % clientCount == clientIndex)
                    .asLongStream()
                    .sum();
            given().when().get(CONTRACTS_BASE + CONTRACT_TOTAL + "?clientId={clientId}", clients.get(index))
                    .then().statusCode(200)
                    .body(equalTo(expectedTotal + ".00"));
        }

        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from contracts.contract
                where start_date = ? and end_date is null and cost_amount between 1 and 50000
                """, Long.class, now.atZone(clock.getZone()).toOffsetDateTime())).isEqualTo(lines);
        assertThat(jdbcTemplate.queryForObject("""
                select count(distinct client_id) from contracts.contract
                """, Long.class)).isEqualTo(clientCount);
    }

    @Test
    @DisplayName("GIVEN a line starting with a hash WHEN the job runs THEN it is rejected like any malformed line and the job completes")
    void shouldRejectLineStartingWithHash() {
        final String jobId = submitAccepted(HEADER + clientId + ";;;100.00\n#garbage;;;1\n");

        awaitStatus(jobId, ContractImportJobStatus.COMPLETED);

        getJob(jobId).body("processedLines", equalTo(2)).body("importedCount", equalTo(1)).body("rejectedCount", equalTo(1));
        given().when().get(JOB_PATH + "/rejections", jobId)
                .then().statusCode(200)
                .body("content.lineNumber", equalTo(List.of(3)))
                .body("content.code", equalTo(List.of("invalidLineFormat")));
    }

    @Test
    @DisplayName("GIVEN a date PostgreSQL cannot store WHEN the job runs THEN only this line is rejected")
    void shouldRejectOnlyTheLineWithADateOutOfRange() {
        final String jobId = submitAccepted(HEADER
                + clientId + ";;;100.00\n"
                + clientId + ";+999999-01-01T00:00:00;;100.00\n"
                + clientId + ";;-000001-01-01T00:00:00;100.00\n"
                + clientId + ";;;100.00\n");

        awaitStatus(jobId, ContractImportJobStatus.COMPLETED);

        getJob(jobId).body("importedCount", equalTo(2)).body("rejectedCount", equalTo(2));
        given().when().get(JOB_PATH + "/rejections", jobId)
                .then().statusCode(200)
                .body("content.code", equalTo(List.of("invalidContractPeriod", "invalidContractPeriod")));
    }

    @Test
    @DisplayName("GIVEN a NUL character WHEN the job runs THEN only this line is rejected")
    void shouldRejectOnlyTheLineWithANulCharacter() {
        final String jobId = submitAccepted(HEADER + clientId + ";;;100.00\nab\u0000c;;;100.00\n" + clientId + ";;;100.00\n");

        awaitStatus(jobId, ContractImportJobStatus.COMPLETED);

        getJob(jobId).body("importedCount", equalTo(2)).body("rejectedCount", equalTo(1));
        given().when().get(JOB_PATH + "/rejections", jobId)
                .then().statusCode(200)
                .body("content.code", equalTo(List.of("invalidLineFormat")));
    }

    @Test
    @DisplayName("GIVEN exactly one full chunk followed by a blank line WHEN the job runs THEN the job and its batch execution both complete")
    void shouldCompleteFullChunkFollowedByBlankLine() {
        final int lines = ContractImportBatchConfiguration.CHUNK_SIZE;

        final String jobId = submitAccepted(HEADER + (clientId + ";;;100.00\n").repeat(lines) + "\n");

        awaitStatus(jobId, ContractImportJobStatus.COMPLETED);
        getJob(jobId).body("importedCount", equalTo(lines));
        await().atMost(Duration.ofSeconds(10)).until(() -> "COMPLETED".equals(jdbcTemplate.queryForObject("""
                select e.status from contracts.batch_job_execution e
                join contracts.batch_job_execution_params p on p.job_execution_id = e.job_execution_id
                where p.parameter_value = ?
                """, String.class, jobId)));
    }

    @Test
    @DisplayName("GIVEN only malformed lines WHEN the job runs THEN it completes without contract")
    void shouldCompleteWithOnlyMalformedLines() {
        final String jobId = submitAccepted(HEADER + "garbage\nmore garbage\n");

        awaitStatus(jobId, ContractImportJobStatus.COMPLETED);
        getJob(jobId).body("importedCount", equalTo(0)).body("rejectedCount", equalTo(2));
    }

    @Test
    @DisplayName("GIVEN a batch already processed WHEN it is processed again THEN it is rejected and no contract is duplicated")
    void shouldRejectSameBatchTwice() {
        final LocalDateTime now = LocalDateTime.now(clock);
        final byte[] file = (HEADER + clientId + ";;;100.00\n" + clientId + ";;;100.00\n").getBytes(StandardCharsets.UTF_8);
        final ContractImportJob job = persistInOneTransaction(() -> {
            final ContractImportJob inserted = jobRepository.insert(ContractImportJob.of("contracts.csv", 2, now), file);
            return jobRepository.save(inserted.start(clock.instant().plus(Duration.ofHours(1))));
        });
        final ProcessContractImportBatch.Command batch = new ProcessContractImportBatch.Command(
                job.getId(), List.of(new RawLine(2, clientId + ";;;100.00")));

        processBatch.execute(batch);

        assertThatThrownBy(() -> processBatch.execute(batch)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(contractsOfClient()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN a file with a wrong header WHEN submitting THEN 422 and nothing is stored")
    void shouldRejectWrongHeader() {
        submit("wrong;header\n" + clientId + ";;;100.00\n")
                .statusCode(422)
                .body("code", equalTo("invalidImportFile"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from contracts.contract_import_job", Long.class)).isZero();
    }

    @Test
    @DisplayName("GIVEN a file over the size limit WHEN submitting THEN 413")
    void shouldRejectFileTooLarge() {
        submit(new byte[50 * 1024 * 1024 + 1])
                .statusCode(413)
                .body("code", equalTo("importFileTooLarge"));
    }

    @Test
    @DisplayName("GIVEN no file part WHEN submitting THEN 400")
    void shouldRejectMissingFilePart() {
        given()
                .contentType(MULTIPART)
                .multiPart("other", "contracts.csv", new byte[]{1})
                .when().post(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(400)
                .body("code", equalTo("missingParameter"));
    }

    @Test
    @DisplayName("GIVEN an unknown job WHEN reading it or its rejections THEN 404")
    void shouldReturn404ForUnknownJob() {
        final UUID unknown = UUID.randomUUID();

        given().when().get(JOB_PATH, unknown)
                .then().statusCode(404).body("code", equalTo("contractImportJobNotFound"));
        given().when().get(JOB_PATH + "/rejections", unknown)
                .then().statusCode(404).body("code", equalTo("contractImportJobNotFound"));
    }

    @Test
    @DisplayName("GIVEN a page size out of bounds WHEN listing rejections THEN 400")
    void shouldRejectPageSizeOutOfBounds() {
        final String jobId = submitAccepted(HEADER + "garbage\n");

        given().queryParam("size", 101).when().get(JOB_PATH + "/rejections", jobId)
                .then().statusCode(400).body("code", equalTo("invalidParameter"));
    }

    @Test
    @DisplayName("GIVEN no token WHEN submitting THEN 401")
    void shouldRequireAuthentication() {
        given()
                .auth().none()
                .contentType(MULTIPART)
                .multiPart("file", "contracts.csv", (HEADER + "garbage\n").getBytes(StandardCharsets.UTF_8))
                .when().post(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(401);
    }
}
