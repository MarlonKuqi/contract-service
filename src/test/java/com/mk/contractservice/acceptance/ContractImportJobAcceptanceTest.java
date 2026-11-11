package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.batch.ContractImportBatchConfiguration;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mk.contractservice.controllers.contract.shared.ContractEndpoints.CONTRACTS_BASE;
import static com.mk.contractservice.controllers.contract.shared.ContractEndpoints.CONTRACT_IMPORT_JOBS;
import static com.mk.contractservice.controllers.contract.shared.ContractEndpoints.CONTRACT_TOTAL;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.contract-import.worker-enabled=true"
)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Feature: A counselor imports a large CSV file of contracts asynchronously")
class ContractImportJobAcceptanceTest {

    private static final String HEADER = "clientId;type;startDate;endDate;costAmount\n";
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
    private JdbcTemplate jdbcTemplate;

    private String clientId;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);
        cleanUp();
        clientId = createCompany();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from contracts.contract_import_job");
        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();
    }

    private String createCompany() {
        return given()
                .body("""
                        {
                            "type": "COMPANY",
                            "name": "Acme SA",
                            "email": "%s",
                            "phone": "%s",
                            "companyIdentifier": "%s"
                        }
                        """.formatted(TestDataHelper.uniqueEmail("acme"),
                        TestDataHelper.randomSwissPhoneNumber(),
                        TestDataHelper.uniqueCompanyIdentifier("CHE")))
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
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
                        """.formatted(TestDataHelper.uniqueEmail("marie.durand"),
                        TestDataHelper.randomSwissPhoneNumber()))
                .when()
                .post(ClientEndpoints.CLIENTS_BASE)
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    private ValidatableResponse submit(final byte[] content) {
        return given()
                .contentType(MULTIPART)
                .multiPart("file", "contracts.csv", content)
                .when()
                .post(CONTRACT_IMPORT_JOBS)
                .then();
    }

    private ValidatableResponse submit(final String content) {
        return submit(content.getBytes(StandardCharsets.UTF_8));
    }

    private String submitAccepted(final String content) {
        return submit(content).statusCode(202).extract().path("jobId");
    }

    private void awaitStatus(final String jobId, final ContractImportJobState.Status status) {
        awaitStatus(jobId, status, Duration.ofSeconds(30));
    }

    private void awaitStatus(final String jobId, final ContractImportJobState.Status status, final Duration timeout) {
        await().atMost(timeout)
                .pollInterval(Duration.ofMillis(100))
                .until(() -> status.name()
                        .equals(
                                given().when().get(JOB_PATH, jobId).then().statusCode(200).extract().path("status")));
    }

    private ValidatableResponse getJob(final String jobId) {
        return given().when().get(JOB_PATH, jobId).then().statusCode(200);
    }

    private int contractsOfClient() {
        return given().when()
                .get(CONTRACTS_BASE + "?clientId={clientId}", clientId)
                .then()
                .statusCode(anyOf(is(200), is(206)))
                .extract()
                .path("totalElements");
    }

    private void assertNoJobStored() {
        given().when()
                .get(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(0));
    }

    @Test
    @DisplayName(
        "GIVEN a valid and a rejected line WHEN the job runs THEN valid lines are imported and the others are reported")
    void shouldImportValidLinesAndReportOthers() {
        final String unknownClient = UUID.randomUUID().toString();
        final String jobId = given()
                .contentType(MULTIPART)
                .multiPart("file",
                        "contracts.csv",
                        (HEADER + clientId + ";LAA;;;100.00\n" + clientId + ";LAA;;;-1\n" + "\n" + unknownClient + ";LAA;;;100.00\n" + "garbage\n" + clientId + ";LAA;2026-01-01T00:00:00;2025-01-01T00:00:00;100.00\n")
                                .getBytes(StandardCharsets.UTF_8))
                .when()
                .post(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(202)
                .header("Location", containsString(CONTRACT_IMPORT_JOBS + "/"))
                .body("jobId", notNullValue())
                .body("status", equalTo(ContractImportJobState.Status.PENDING.name()))
                .body("totalLines", equalTo(5))
                .extract()
                .path("jobId");

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);

        getJob(jobId)
                .body("fileName", equalTo("contracts.csv"))
                .body("processedLines", equalTo(5))
                .body("importedCount", equalTo(1))
                .body("rejectedCount", equalTo(4))
                .body("finishedAt", notNullValue());
        assertThat(contractsOfClient()).isEqualTo(1);

        given().when()
                .get(JOB_PATH + "/rejections", jobId)
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(4))
                .body("content.lineNumber", equalTo(List.of(3, 5, 6, 7)))
                .body("content.code",
                        equalTo(List.of("invalidContractCost",
                                "clientNotFound",
                                "invalidLineFormat",
                                "invalidContractPeriod")))
                .body("content[0].clientId", equalTo(clientId))
                .body("content[1].clientId", equalTo(unknownClient))
                .body("content[2].clientId", equalTo(null));
    }

    @Test
    @DisplayName("GIVEN more lines than a batch WHEN the job runs THEN every line is imported")
    void shouldImportAcrossBatches() {
        final int lines = ContractImportBatchConfiguration.CHUNK_SIZE + 1;

        final String jobId = submitAccepted(HEADER + (clientId + ";LAA;;;100.00\n").repeat(lines));

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);
        getJob(jobId).body("importedCount", equalTo(lines));
        assertThat(contractsOfClient()).isEqualTo(lines);
    }

    @Test
    @DisplayName(
        "GIVEN 10000 contracts for one client costing 1 to 10000 WHEN the job runs THEN all are active from now and their total is right")
    void shouldImportTenThousandContractsForOneClient() {
        final int lines = 10_000;
        final LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        final String contracts = IntStream.rangeClosed(1, lines)
                .mapToObj(cost -> clientId + ";LAA;" + now + ";;" + cost + "\n")
                .collect(Collectors.joining());

        final String jobId = submitAccepted(HEADER + contracts);

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);
        getJob(jobId)
                .body("processedLines", equalTo(lines))
                .body("importedCount", equalTo(lines))
                .body("rejectedCount", equalTo(0));

        given().when()
                .get(CONTRACTS_BASE + CONTRACT_TOTAL + "?clientId={clientId}", clientId)
                .then()
                .statusCode(200)
                .body(equalTo("50005000.00"));

        assertThat(contractsOfClient()).isEqualTo(lines);
        given().when()
                .get(CONTRACTS_BASE + "?clientId={clientId}&size=100", clientId)
                .then()
                .statusCode(206)
                .body("content.startDate", everyItem(equalTo(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
                .body("content.endDate", everyItem(nullValue()));
    }

    @Test
    @DisplayName(
        "GIVEN a line starting with a hash WHEN the job runs THEN it is rejected like any malformed line and the job completes")
    void shouldRejectLineStartingWithHash() {
        final String jobId = submitAccepted(HEADER + clientId + ";LAA;;;100.00\n#garbage;LAA;;;1\n");

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);

        getJob(jobId).body("processedLines", equalTo(2))
                .body("importedCount", equalTo(1))
                .body("rejectedCount", equalTo(1));
        given().when()
                .get(JOB_PATH + "/rejections", jobId)
                .then()
                .statusCode(200)
                .body("content.lineNumber", equalTo(List.of(3)))
                .body("content.code", equalTo(List.of("invalidLineFormat")));
    }

    @Test
    @DisplayName(
        "GIVEN a health insurance (LAMal) and an unknown type for a company WHEN the job runs THEN only the employer insurance (LAA) is imported")
    void shouldRejectTypesNotAllowedForTheClient() {
        final String jobId = submitAccepted(
                HEADER + clientId + ";LAA;;;100.00\n" + clientId + ";LAMAL;;;100.00\n" + clientId + ";CASCO;;;100.00\n");

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);

        getJob(jobId).body("importedCount", equalTo(1))
                .body("rejectedCount", equalTo(2));
        assertThat(contractsOfClient()).isEqualTo(1);
        given().when()
                .get(JOB_PATH + "/rejections", jobId)
                .then()
                .statusCode(200)
                .body("content.lineNumber", equalTo(List.of(3, 4)))
                .body("content.code", equalTo(List.of("contractTypeNotAllowed", "invalidLineFormat")));
    }

    @Test
    @DisplayName(
        "GIVEN LAMal lines overlapping a stored LAMal or a previous line of the file WHEN the job runs THEN only those lines are rejected")
    void shouldRejectOverlappingLamalLines() {
        final String personId = createPerson();
        final String otherPersonId = createPerson();
        final String firstJob = submitAccepted(HEADER + personId + ";LAMAL;;;300.00\n");
        awaitStatus(firstJob, ContractImportJobState.Status.COMPLETED);

        final String jobId = submitAccepted(
                HEADER + personId + ";LAMAL;2030-01-01T00:00:00;;300.00\n" + otherPersonId + ";LAMAL;2030-01-01T00:00:00;2031-01-01T00:00:00;300.00\n" + otherPersonId + ";LAMAL;2031-01-01T00:00:00;;300.00\n" + otherPersonId + ";LAMAL;2030-06-01T00:00:00;;300.00\n");

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);

        getJob(jobId).body("importedCount", equalTo(2)).body("rejectedCount", equalTo(2));
        given().when()
                .get(JOB_PATH + "/rejections", jobId)
                .then()
                .statusCode(200)
                .body("content.lineNumber", equalTo(List.of(2, 5)))
                .body("content.code", equalTo(List.of("contractOverlap", "contractOverlap")));
    }

    @Test
    @DisplayName("GIVEN a date PostgreSQL cannot store WHEN the job runs THEN only this line is rejected")
    void shouldRejectOnlyTheLineWithADateOutOfRange() {
        final String jobId = submitAccepted(
                HEADER + clientId + ";LAA;;;100.00\n" + clientId + ";LAA;+999999-01-01T00:00:00;;100.00\n" + clientId + ";LAA;;-000001-01-01T00:00:00;100.00\n" + clientId + ";LAA;;;100.00\n");

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);

        getJob(jobId).body("importedCount", equalTo(2)).body("rejectedCount", equalTo(2));
        given().when()
                .get(JOB_PATH + "/rejections", jobId)
                .then()
                .statusCode(200)
                .body("content.code", equalTo(List.of("invalidContractPeriod", "invalidContractPeriod")));
    }

    @Test
    @DisplayName("GIVEN a NUL character WHEN the job runs THEN only this line is rejected")
    void shouldRejectOnlyTheLineWithANulCharacter() {
        final String jobId = submitAccepted(
                HEADER + clientId + ";LAA;;;100.00\nab\u0000c;;;100.00\n" + clientId + ";LAA;;;100.00\n");

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);

        getJob(jobId).body("importedCount", equalTo(2)).body("rejectedCount", equalTo(1));
        given().when()
                .get(JOB_PATH + "/rejections", jobId)
                .then()
                .statusCode(200)
                .body("content.code", equalTo(List.of("invalidLineFormat")));
    }

    @Test
    @DisplayName("GIVEN only malformed lines WHEN the job runs THEN it completes without contract")
    void shouldCompleteWithOnlyMalformedLines() {
        final String jobId = submitAccepted(HEADER + "garbage\nmore garbage\n");

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);
        getJob(jobId).body("importedCount", equalTo(0)).body("rejectedCount", equalTo(2));
    }

    @Test
    @DisplayName("GIVEN a file sent with an empty name WHEN submitting THEN the job is stored under the default name")
    void shouldUseDefaultNameForUnnamedFile() {
        given()
                .contentType(MULTIPART)
                .multiPart("file", "", (HEADER + clientId + ";LAA;;;100.00\n").getBytes(StandardCharsets.UTF_8))
                .when()
                .post(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(202)
                .body("fileName", equalTo("import.csv"));
    }

    @Test
    @DisplayName("GIVEN a file with a wrong header WHEN submitting THEN 422 and nothing is stored")
    void shouldRejectWrongHeader() {
        submit("wrong;header\n" + clientId + ";LAA;;;100.00\n")
                .statusCode(422)
                .body("code", equalTo("invalidImportFile"));

        assertNoJobStored();
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
                .when()
                .post(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(400)
                .body("code", equalTo("missingParameter"));
    }

    @Test
    @DisplayName("GIVEN an unknown job WHEN reading it or its rejections THEN 404")
    void shouldReturn404ForUnknownJob() {
        final UUID unknown = UUID.randomUUID();

        given().when()
                .get(JOB_PATH, unknown)
                .then()
                .statusCode(404)
                .body("code", equalTo("contractImportJobNotFound"));
        given().when()
                .get(JOB_PATH + "/rejections", unknown)
                .then()
                .statusCode(404)
                .body("code", equalTo("contractImportJobNotFound"));
    }

    @Test
    @DisplayName("GIVEN a page size out of bounds WHEN listing rejections THEN 400")
    void shouldRejectPageSizeOutOfBounds() {
        final String jobId = submitAccepted(HEADER + "garbage\n");

        given().queryParam("size", 101)
                .when()
                .get(JOB_PATH + "/rejections", jobId)
                .then()
                .statusCode(400)
                .body("code", equalTo("invalidParameter"));
    }

    @Test
    @DisplayName("GIVEN no token WHEN submitting THEN 401")
    void shouldRequireAuthentication() {
        given()
                .auth()
                .none()
                .contentType(MULTIPART)
                .multiPart("file", "contracts.csv", (HEADER + "garbage\n").getBytes(StandardCharsets.UTF_8))
                .when()
                .post(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(401);
    }
}
