package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.acceptance.helper.TestTokens;
import com.mk.contractservice.batch.ContractImportBatchConfiguration;
import com.mk.contractservice.batch.ContractImportJobRunner;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractImportRejectionCsv;
import com.mk.contractservice.domain.contract.importjob.IdempotencyKey;
import com.mk.contractservice.features.contract.importjob.ClaimContractImportJob;
import com.mk.contractservice.features.contract.importjob.ProcessContractImportBatch;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mk.contractservice.controllers.contract.shared.ContractEndpoints.CONTRACTS_BASE;
import static com.mk.contractservice.controllers.contract.shared.ContractEndpoints.CONTRACT_BY_ID;
import static com.mk.contractservice.controllers.contract.shared.ContractEndpoints.CONTRACT_IMPORT_JOBS;
import static com.mk.contractservice.controllers.contract.shared.ContractEndpoints.CONTRACT_TOTAL;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Feature: A counselor follows, cancels and fixes the asynchronous import jobs")
class ContractImportJobManagementAcceptanceTest {

    private static final String HEADER = "clientId;startDate;endDate;costAmount\n";
    private static final String MULTIPART = "multipart/form-data";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final String JOB_PATH = CONTRACT_IMPORT_JOBS + "/{jobId}";
    private static final String CANCELLATION_PATH = JOB_PATH + "/cancellation";
    private static final String REJECTIONS_EXPORT_PATH = JOB_PATH + "/rejections/export";
    private static final String OTHER_COUNSELOR = "paul.counselor";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ClientJpaRepository clientJpaRepository;

    @Autowired
    private ContractJpaRepository contractJpaRepository;

    @Autowired
    private ClaimContractImportJob claimContractImportJob;

    @Autowired
    private ContractImportJobRunner contractImportJobRunner;

    @MockitoSpyBean
    private ProcessContractImportBatch processBatch;

    private String clientId;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);
        cleanUp();
        clientId = given()
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

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from contracts.contract_import_job");
        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();
    }

    private String fileOf(final List<String> costs) {
        final StringBuilder file = new StringBuilder(HEADER);
        for (final String cost : costs) {
            file.append(clientId).append(";;;").append(cost).append('\n');
        }
        return file.toString();
    }

    private String fileOf(final String... costs) {
        return fileOf(List.of(costs));
    }

    private RequestSpecification upload(final String content) {
        return given()
                .contentType(MULTIPART)
                .multiPart("file", "contracts.csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private RequestSpecification uploadAsOtherCounselor(final String content) {
        return upload(content)
                .auth()
                .oauth2(TestTokens.builder().realmRoles(TestTokens.COUNSELOR_ROLE).username(OTHER_COUNSELOR).build());
    }

    private ValidatableResponse submit(final String content, final String idempotencyKey) {
        return upload(content).header(IDEMPOTENCY_KEY, idempotencyKey).when().post(CONTRACT_IMPORT_JOBS).then();
    }

    private String submitAccepted(final String content) {
        return upload(content).when().post(CONTRACT_IMPORT_JOBS).then().statusCode(202).extract().path("jobId");
    }

    private void runNextJob() {
        claimContractImportJob.execute().ifPresent(job -> contractImportJobRunner.run(job.id()));
    }

    private ValidatableResponse getJob(final String jobId) {
        return given().when().get(JOB_PATH, jobId).then().statusCode(200);
    }

    private ValidatableResponse cancel(final String jobId) {
        return given().when().post(CANCELLATION_PATH, jobId).then();
    }

    private ValidatableResponse totalOfClient() {
        return given().when()
                .get(CONTRACTS_BASE + CONTRACT_TOTAL + "?clientId={clientId}", clientId)
                .then()
                .statusCode(200);
    }

    private int jobCount() {
        return jdbcTemplate.queryForObject("select count(*) from contracts.contract_import_job", Integer.class);
    }

    @Test
    @DisplayName(
        "GIVEN a file submitted again with the same key after a timeout WHEN it runs THEN one job imports each contract once")
    void shouldImportOnceWhenSubmittedTwiceWithSameKey() {
        final String file = fileOf("100.00", "200.00");

        final String first = submit(file, "import-42").statusCode(202).extract().path("jobId");
        submit(file, "import-42")
                .statusCode(202)
                .header("Location", endsWith(first))
                .body("jobId", equalTo(first));
        runNextJob();
        runNextJob();

        assertThat(jobCount()).isEqualTo(1);
        assertThat(contractJpaRepository.count()).isEqualTo(2);
        getJob(first).body("status", equalTo("COMPLETED")).body("submittedBy", equalTo(TestTokens.DEFAULT_USERNAME));
    }

    @Test
    @DisplayName("GIVEN a key already used for another file WHEN submitting THEN 422 and nothing is stored")
    void shouldRefuseKeyReusedForAnotherFile() {
        submit(fileOf("100.00"), "import-42").statusCode(202);

        submit(fileOf("999.00"), "import-42")
                .statusCode(422)
                .body("code", equalTo("idempotencyKeyReused"));

        assertThat(jobCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN two counselors using the same key WHEN submitting THEN each one gets its own job")
    void shouldScopeKeyToCounselor() {
        final String file = fileOf("100.00");
        final String mine = submit(file, "import-42").statusCode(202).extract().path("jobId");

        final String theirs = uploadAsOtherCounselor(file)
                .header(IDEMPOTENCY_KEY, "import-42")
                .when()
                .post(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(202)
                .body("submittedBy", equalTo(OTHER_COUNSELOR))
                .extract()
                .path("jobId");

        assertThat(theirs).isNotEqualTo(mine);
    }

    @Test
    @DisplayName("GIVEN a key longer than allowed WHEN submitting THEN 400 and nothing is stored")
    void shouldRefuseTooLongKey() {
        submit(fileOf("100.00"), "k".repeat(IdempotencyKey.MAX_LENGTH + 1))
                .statusCode(400)
                .body("code", equalTo("invalidIdempotencyKey"));

        assertThat(jobCount()).isZero();
    }

    @Test
    @DisplayName(
        "GIVEN jobs of two counselors WHEN listing THEN they come newest first, and can be narrowed to one counselor")
    void shouldListJobsNewestFirst() {
        final String oldest = submitAccepted(fileOf("100.00"));
        final String theirs = uploadAsOtherCounselor(fileOf("200.00"))
                .when()
                .post(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(202)
                .extract()
                .path("jobId");
        final String newest = submitAccepted(fileOf("300.00"));

        given().when()
                .get(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(3))
                .body("content.jobId", contains(newest, theirs, oldest))
                .body("content.submittedBy",
                        contains(TestTokens.DEFAULT_USERNAME, OTHER_COUNSELOR, TestTokens.DEFAULT_USERNAME));

        given().queryParam("submittedBy", OTHER_COUNSELOR)
                .when()
                .get(CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(200)
                .body("content.jobId", contains(theirs));
    }

    @Test
    @DisplayName(
        "GIVEN an imported file WHEN the counselor cancels the job THEN its contracts are closed and leave the client total")
    void shouldCloseContractsOfCancelledJob() {
        final String jobId = submitAccepted(fileOf("100.00", "200.00"));
        runNextJob();
        totalOfClient().body(equalTo("300.00"));
        final UUID contractId = contractJpaRepository.findAll().getFirst().getId();

        cancel(jobId)
                .statusCode(200)
                .body("closedContracts", equalTo(2))
                .body("job.status", equalTo("CANCELLED"))
                .body("job.cancelledBy", equalTo(TestTokens.DEFAULT_USERNAME))
                .body("job.cancelledAt", notNullValue())
                .body("job.importedCount", equalTo(2));

        totalOfClient().body(anyOf(equalTo("0"), equalTo("0.00"), equalTo("0.0")));
        given().when()
                .get(CONTRACTS_BASE + "?clientId={clientId}", clientId)
                .then()
                .statusCode(200)
                .body("content", hasSize(0));
        given().when().get(CONTRACT_BY_ID, contractId).then().statusCode(200).body("endDate", notNullValue());
    }

    @Test
    @DisplayName("GIVEN a cancelled job WHEN it is cancelled again THEN nothing changes")
    void shouldIgnoreSecondCancellation() {
        final String jobId = submitAccepted(fileOf("100.00"));
        runNextJob();
        cancel(jobId).statusCode(200);
        final String cancelledAt = getJob(jobId).extract().path("cancelledAt");

        cancel(jobId)
                .statusCode(200)
                .body("closedContracts", equalTo(0))
                .body("job.cancelledBy", equalTo(TestTokens.DEFAULT_USERNAME));

        getJob(jobId).body("cancelledAt", equalTo(cancelledAt));
    }

    @Test
    @DisplayName("GIVEN a job still waiting WHEN the counselor cancels it THEN it never runs")
    void shouldNeverRunCancelledPendingJob() {
        final String jobId = submitAccepted(fileOf("100.00"));

        cancel(jobId).statusCode(200).body("closedContracts", equalTo(0)).body("job.status", equalTo("CANCELLED"));
        runNextJob();

        assertThat(contractJpaRepository.count()).isZero();
        getJob(jobId).body("status", equalTo("CANCELLED")).body("processedLines", equalTo(0));
    }

    @Test
    @DisplayName(
        "GIVEN a job running its second chunk WHEN the counselor cancels it THEN the chunk is dropped and the first one is closed")
    void shouldStopRunningJobOnCancellation() throws Exception {
        final int chunkSize = ContractImportBatchConfiguration.CHUNK_SIZE;
        final String jobId = submitAccepted(fileOf(Collections.nCopies(chunkSize + 10, "10.00")));
        final CountDownLatch secondChunkStarted = new CountDownLatch(1);
        final CountDownLatch cancelled = new CountDownLatch(1);
        final AtomicInteger chunks = new AtomicInteger();
        doAnswer(invocation -> {
            if (chunks.incrementAndGet() == 2) {
                secondChunkStarted.countDown();
                assertThat(cancelled.await(30, TimeUnit.SECONDS)).isTrue();
            }
            return invocation.callRealMethod();
        }).when(processBatch).execute(any());
        final CompletableFuture<Void> run = CompletableFuture.runAsync(this::runNextJob);

        assertThat(secondChunkStarted.await(30, TimeUnit.SECONDS)).isTrue();
        cancel(jobId).statusCode(200).body("closedContracts", equalTo(chunkSize));
        cancelled.countDown();
        run.get(60, TimeUnit.SECONDS);

        assertThat(contractJpaRepository.count()).isEqualTo(chunkSize);
        totalOfClient().body(anyOf(equalTo("0"), equalTo("0.00"), equalTo("0.0")));
        getJob(jobId)
                .body("status", equalTo("CANCELLED"))
                .body("importedCount", equalTo(chunkSize))
                .body("failureCode", nullValue());
    }

    @Test
    @DisplayName(
        "GIVEN rejected lines WHEN the counselor downloads them THEN each comes with its code, its reason and its original line")
    void shouldExportRejectedLinesAsCsv() {
        final String jobId = submitAccepted(
                HEADER + clientId + ";;;100.00\n" + clientId + ";;;-1\n" + "garbage \"quoted\"\n");
        runNextJob();

        final String csv = given().when()
                .get(REJECTIONS_EXPORT_PATH, jobId)
                .then()
                .statusCode(200)
                .contentType(startsWith("text/csv"))
                .header("Content-Disposition", containsString("attachment"))
                .extract()
                .asString();

        final List<String> lines = csv.lines().toList();
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).isEqualTo(ContractImportRejectionCsv.HEADER);
        assertThat(lines.get(1)).startsWith("3;invalidContractCost;\"").endsWith(";\"" + clientId + ";;;-1\"");
        assertThat(lines.get(2)).startsWith("4;invalidLineFormat;\"").endsWith(";\"garbage \"\"quoted\"\"\"");
    }

    @Test
    @DisplayName("GIVEN an unknown job WHEN cancelling it or downloading its rejections THEN 404")
    void shouldAnswerNotFoundForUnknownJob() {
        final String unknown = UUID.randomUUID().toString();

        cancel(unknown).statusCode(404).body("code", equalTo("contractImportJobNotFound"));
        given().when()
                .get(REJECTIONS_EXPORT_PATH, unknown)
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("code", equalTo("contractImportJobNotFound"));
    }
}
