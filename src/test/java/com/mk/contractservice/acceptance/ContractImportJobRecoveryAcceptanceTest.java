package com.mk.contractservice.acceptance;

import com.mk.contractservice.acceptance.config.TestcontainersConfiguration;
import com.mk.contractservice.acceptance.helper.AcceptanceRestAssured;
import com.mk.contractservice.acceptance.helper.TestDataHelper;
import com.mk.contractservice.batch.ContractImportBatchConfiguration;
import com.mk.contractservice.batch.ContractImportJobRunner;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState;
import com.mk.contractservice.domain.contract.importjob.ContractImportSubmission;
import com.mk.contractservice.features.contract.importjob.ClaimContractImportJob;
import com.mk.contractservice.features.contract.importjob.ProcessContractImportBatch;
import com.mk.contractservice.features.shared.ContractImportCsv.RawLine;
import com.mk.contractservice.infrastructure.persistence.client.ClientJpaRepository;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.contract-import.worker-enabled=true"
)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Technical guarantee: an import job stops cleanly on failure and resumes after a crash without duplicates")
class ContractImportJobRecoveryAcceptanceTest {

    private static final String HEADER = "clientId;type;franchise;startDate;endDate;costAmount\n";
    private static final String JOB_PATH = ContractEndpoints.CONTRACT_IMPORT_JOBS + "/{jobId}";
    private static final int CHUNK_SIZE = ContractImportBatchConfiguration.CHUNK_SIZE;
    private static final String TAKEOVER_THREAD = "takeover-worker";
    private static final Duration STALL_LIMIT = Duration.ofSeconds(60);

    @LocalServerPort
    private int port;

    @Autowired
    private Clock clock;

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

    @Autowired
    private ContractImportJobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private ProcessContractImportBatch processBatch;

    private final AtomicReference<RuntimeException> secondChunkFailure = new AtomicReference<>(
            new IllegalStateException("Simulated failure"));
    private final AtomicReference<RuntimeException> everyChunkFailure = new AtomicReference<>();
    private final AtomicBoolean secondChunkInterrupted = new AtomicBoolean();
    private final AtomicInteger interruptionsLeft = new AtomicInteger();
    private final AtomicBoolean firstWorkerStalls = new AtomicBoolean();
    private final CountDownLatch firstWorkerStalled = new CountDownLatch(1);
    private final CountDownLatch wakeFirstWorker = new CountDownLatch(1);
    private final CountDownLatch takeoverWorkerStalled = new CountDownLatch(1);
    private final CountDownLatch wakeTakeoverWorker = new CountDownLatch(1);

    private String clientId;

    @BeforeEach
    void setUp() {
        AcceptanceRestAssured.configure(port);
        deleteTestData();
        clientId = given()
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

        final AtomicInteger chunks = new AtomicInteger();
        final AtomicInteger firstWorkerChunks = new AtomicInteger();
        final AtomicInteger takeoverWorkerChunks = new AtomicInteger();
        doAnswer(invocation -> {
            if (firstWorkerStalls.get()) {
                if (Thread.currentThread().getName().equals(TAKEOVER_THREAD)) {
                    stallOnSecondChunk(takeoverWorkerChunks, takeoverWorkerStalled, wakeTakeoverWorker);
                } else {
                    stallOnSecondChunk(firstWorkerChunks, firstWorkerStalled, wakeFirstWorker);
                }
                return invocation.callRealMethod();
            }
            if (interruptionsLeft.getAndUpdate(left -> Math.max(0, left - 1)) > 0) {
                Thread.currentThread().interrupt();
                return invocation.callRealMethod();
            }
            final RuntimeException everyChunk = everyChunkFailure.get();
            if (everyChunk != null) {
                throw everyChunk;
            }
            if (chunks.incrementAndGet() == 2) {
                if (secondChunkInterrupted.get()) {
                    Thread.currentThread().interrupt();
                } else if (secondChunkFailure.get() != null) {
                    throw secondChunkFailure.get();
                }
            }
            return invocation.callRealMethod();
        }).when(processBatch).execute(any());
    }

    private static void stallOnSecondChunk(final AtomicInteger chunks,
            final CountDownLatch stalled,
            final CountDownLatch wake) throws InterruptedException {
        if (chunks.incrementAndGet() == 2) {
            stalled.countDown();
            wake.await(STALL_LIMIT.toSeconds(), TimeUnit.SECONDS);
        }
    }

    @AfterEach
    void cleanUp() {
        wakeFirstWorker.countDown();
        wakeTakeoverWorker.countDown();
        deleteTestData();
    }

    private void deleteTestData() {
        jdbcTemplate.update("delete from contracts.contract_import_job");
        contractJpaRepository.deleteAll();
        clientJpaRepository.deleteAll();
    }

    private String submitLines(final int lines) {
        return given()
                .contentType("multipart/form-data")
                .multiPart("file",
                        "contracts.csv",
                        (HEADER + (clientId + ";LAA;;;;100.00\n").repeat(lines)).getBytes(StandardCharsets.UTF_8))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(202)
                .extract()
                .path("jobId");
    }

    private void awaitStatus(final String jobId, final ContractImportJobState.Status status) {
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> status.name()
                        .equals(
                                given().when().get(JOB_PATH, jobId).then().statusCode(200).extract().path("status")));
    }

    private long contractsOfClient() {
        return jdbcTemplate.queryForObject(
                "select count(*) from contracts.contract where client_id = ?::uuid",
                Long.class,
                clientId);
    }

    private long executionsOf(final String jobId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from contracts.batch_job_execution_params where parameter_value = ?",
                Long.class,
                jobId);
    }

    private void expireLeaseOf(final String jobId) {
        jdbcTemplate.update("update contracts.contract_import_job set lease_until = ? where id = ?::uuid",
                LocalDateTime.now(clock).minusHours(1).atZone(clock.getZone()).toOffsetDateTime(),
                jobId);
    }

    private void simulateCrashOf(final String jobId) {
        final String executions = """
                (select e.job_execution_id from contracts.batch_job_execution e
                 join contracts.batch_job_execution_params p on p.job_execution_id = e.job_execution_id
                 where p.parameter_value = ?)
                """;
        jdbcTemplate.update(
                "update contracts.batch_step_execution set status = 'STARTED', exit_code = 'UNKNOWN', end_time = null" + " where job_execution_id in " + executions,
                jobId);
        jdbcTemplate.update(
                "update contracts.batch_job_execution set status = 'STARTED', exit_code = 'UNKNOWN', end_time = null" + " where job_execution_id in " + executions,
                jobId);
        jdbcTemplate.update(
                "update contracts.contract_import_job set status = ?, finished_at = null, failure_reason = null where id = ?::uuid",
                ContractImportJobState.Status.RUNNING.name(),
                jobId);
        expireLeaseOf(jobId);
    }

    @Test
    @DisplayName(
        "GIVEN an unexpected error during a chunk WHEN the job runs THEN it fails and earlier chunks stay imported")
    void shouldFailAndKeepEarlierChunks() {
        final String jobId = submitLines(CHUNK_SIZE + 1);

        awaitStatus(jobId, ContractImportJobState.Status.FAILED);

        given().when()
                .get(JOB_PATH, jobId)
                .then()
                .statusCode(200)
                .body("processedLines", equalTo(CHUNK_SIZE))
                .body("importedCount", equalTo(CHUNK_SIZE))
                .body("failureCode", equalTo("unexpectedError"))
                .body("finishedAt", notNullValue());
        assertThat(contractsOfClient()).isEqualTo(CHUNK_SIZE);
    }

    @Test
    @DisplayName(
        "GIVEN a job interrupted by a crash WHEN its lease expires THEN it resumes after the last chunk without duplicates")
    void shouldResumeAfterCrashWithoutDuplicates() {
        final int lines = CHUNK_SIZE + 1;
        final String jobId = submitLines(lines);
        awaitStatus(jobId, ContractImportJobState.Status.FAILED);

        secondChunkFailure.set(null);
        simulateCrashOf(jobId);

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);

        given().when()
                .get(JOB_PATH, jobId)
                .then()
                .statusCode(200)
                .body("processedLines", equalTo(lines))
                .body("importedCount", equalTo(lines))
                .body("rejectedCount", equalTo(0));
        assertThat(contractsOfClient()).isEqualTo(lines);
        assertThat(jdbcTemplate.queryForObject(
                "select max(line_number) from contracts.contract_import_job_line where job_id = ?::uuid",
                Integer.class,
                jobId))
                .isEqualTo(lines + 1);
    }

    @Test
    @DisplayName(
        "GIVEN a transient database error during a chunk WHEN the job runs THEN it resumes and completes without duplicates")
    void shouldResumeAfterTransientFailure() {
        secondChunkFailure.set(new CannotAcquireLockException("Simulated deadlock"));
        final int lines = CHUNK_SIZE * 2 + 1;

        final String jobId = submitLines(lines);

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);
        given().when().get(JOB_PATH, jobId).then().statusCode(200).body("importedCount", equalTo(lines));
        assertThat(contractsOfClient()).isEqualTo(lines);
    }

    @Test
    @DisplayName("GIVEN a transient error that never stops WHEN the attempts run out THEN the job fails")
    void shouldFailAfterTooManyAttempts() {
        everyChunkFailure.set(new CannotAcquireLockException("Simulated deadlock"));

        final String jobId = submitLines(1);

        awaitStatus(jobId, ContractImportJobState.Status.FAILED);
        given().when().get(JOB_PATH, jobId).then().statusCode(200).body("failureCode", equalTo("tooManyAttempts"));
        assertThat(executionsOf(jobId)).isEqualTo(ContractImportJobRunner.MAX_ATTEMPTS);
    }

    @Test
    @DisplayName("GIVEN a thread interrupted during a job WHEN the step stops THEN no line is lost and the job resumes")
    void shouldResumeAfterThreadInterruption() {
        secondChunkInterrupted.set(true);
        final int lines = CHUNK_SIZE * 3 + 1;

        final String jobId = submitLines(lines);

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);
        given().when()
                .get(JOB_PATH, jobId)
                .then()
                .statusCode(200)
                .body("processedLines", equalTo(lines))
                .body("importedCount", equalTo(lines));
        assertThat(contractsOfClient()).isEqualTo(lines);
    }

    @Test
    @DisplayName(
        "GIVEN a job stopped by more shutdowns than allowed attempts WHEN it runs again THEN it completes without duplicates")
    void shouldNotCountShutdownsAsAttempts() {
        secondChunkFailure.set(null);
        interruptionsLeft.set(ContractImportJobRunner.MAX_ATTEMPTS);
        final int lines = CHUNK_SIZE * (ContractImportJobRunner.MAX_ATTEMPTS + 1);

        final String jobId = submitLines(lines);

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);
        given().when()
                .get(JOB_PATH, jobId)
                .then()
                .statusCode(200)
                .body("processedLines", equalTo(lines))
                .body("importedCount", equalTo(lines));
        assertThat(contractsOfClient()).isEqualTo(lines);
        assertThat(executionsOf(jobId)).isEqualTo(ContractImportJobRunner.MAX_ATTEMPTS + 1);
    }

    @Test
    @DisplayName(
        "GIVEN a stalled worker whose job was taken over WHEN it wakes up THEN it does not fail the job of the other worker")
    void shouldNotLetAStalledWorkerFailTheJobOfTheOtherWorker() throws Exception {
        firstWorkerStalls.set(true);
        final int lines = CHUNK_SIZE * 4 + 1;
        final String jobId = submitLines(lines);
        assertThat(firstWorkerStalled.await(30, TimeUnit.SECONDS)).isTrue();

        expireLeaseOf(jobId);
        final Thread takeoverWorker = new Thread(
                () -> claimContractImportJob.execute()
                        .ifPresent(job -> contractImportJobRunner.run(UUID.fromString(jobId))),
                TAKEOVER_THREAD);
        takeoverWorker.start();
        assertThat(takeoverWorkerStalled.await(30, TimeUnit.SECONDS)).isTrue();

        wakeFirstWorker.countDown();
        await().atMost(Duration.ofSeconds(10)).until(() -> jdbcTemplate.queryForObject("""
                select count(*) from contracts.batch_job_execution e
                join contracts.batch_job_execution_params p on p.job_execution_id = e.job_execution_id
                where p.parameter_value = ? and e.end_time is not null
                """, Long.class, jobId) == 1);
        given().when()
                .get(JOB_PATH, jobId)
                .then()
                .statusCode(200)
                .body("status", equalTo(ContractImportJobState.Status.RUNNING.name()));

        wakeTakeoverWorker.countDown();
        takeoverWorker.join(Duration.ofSeconds(30));

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);
        assertThat(contractsOfClient()).isEqualTo(lines);
    }

    @Test
    @DisplayName(
        "GIVEN a batch execution complete but a job still running with missing lines WHEN its lease expires THEN the job fails instead of running forever")
    void shouldFailJobWhoseExecutionEndedWithMissingLines() {
        secondChunkFailure.set(null);
        final String jobId = submitLines(2);
        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);

        jdbcTemplate.update("""
                update contracts.contract_import_job
                set status = ?, finished_at = null, processed_lines = 1, imported_count = 1
                where id = ?::uuid
                """, ContractImportJobState.Status.RUNNING.name(), jobId);
        expireLeaseOf(jobId);

        awaitStatus(jobId, ContractImportJobState.Status.FAILED);
        given().when().get(JOB_PATH, jobId).then().statusCode(200).body("failureCode", equalTo("incompleteProcessing"));
    }

    @Test
    @DisplayName(
        "GIVEN a batch execution complete and every line processed but a job still running WHEN its lease expires THEN the job completes")
    void shouldCompleteJobWhoseListenerNeverRan() {
        secondChunkFailure.set(null);
        final String jobId = submitLines(2);
        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);

        jdbcTemplate.update(
                "update contracts.contract_import_job set status = ?, finished_at = null where id = ?::uuid",
                ContractImportJobState.Status.RUNNING.name(),
                jobId);
        expireLeaseOf(jobId);

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);
        assertThat(executionsOf(jobId)).isEqualTo(1);
    }

    @Test
    @DisplayName(
        "GIVEN exactly one full chunk followed by a blank line WHEN the job runs THEN the job and its batch execution both complete")
    void shouldCompleteFullChunkFollowedByBlankLine() {
        secondChunkFailure.set(null);
        final String jobId = given()
                .contentType("multipart/form-data")
                .multiPart("file",
                        "contracts.csv",
                        (HEADER + (clientId + ";LAA;;;;100.00\n").repeat(CHUNK_SIZE) + "\n").getBytes(
                                StandardCharsets.UTF_8))
                .when()
                .post(ContractEndpoints.CONTRACT_IMPORT_JOBS)
                .then()
                .statusCode(202)
                .extract()
                .path("jobId");

        awaitStatus(jobId, ContractImportJobState.Status.COMPLETED);
        given().when().get(JOB_PATH, jobId).then().statusCode(200).body("importedCount", equalTo(CHUNK_SIZE));
        await().atMost(Duration.ofSeconds(10)).until(() -> "COMPLETED".equals(jdbcTemplate.queryForObject("""
                select e.status from contracts.batch_job_execution e
                join contracts.batch_job_execution_params p on p.job_execution_id = e.job_execution_id
                where p.parameter_value = ?
                """, String.class, jobId)));
    }

    @Test
    @DisplayName(
        "GIVEN a batch already processed WHEN it is processed again THEN it is rejected and no contract is duplicated")
    void shouldRejectSameBatchTwice() {
        secondChunkFailure.set(null);
        final LocalDateTime now = LocalDateTime.now(clock);
        final byte[] file = (HEADER + clientId + ";LAA;;;;100.00\n" + clientId + ";LAA;;;;100.00\n").getBytes(
                StandardCharsets.UTF_8);
        final ContractImportJob job = new TransactionTemplate(transactionManager).execute(status -> {
            final ContractImportJob inserted = jobRepository.save(ContractImportJob.of(ContractImportSubmission.of(
                    "contracts.csv",
                    "marie.counselor",
                    null,
                    "a".repeat(64),
                    now), 2), file);
            return jobRepository.save(inserted.start(clock.instant().plus(Duration.ofHours(1))));
        });
        final ProcessContractImportBatch.Command batch = new ProcessContractImportBatch.Command(
                job.id(),
                List.of(new RawLine(2, clientId + ";LAA;;;;100.00")));

        processBatch.execute(batch);

        assertThatThrownBy(() -> processBatch.execute(batch)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(contractsOfClient()).isEqualTo(1);
    }
}
