package com.mk.contractservice.batch;

import com.mk.contractservice.domain.contract.importjob.ContractImportFailureReason;
import com.mk.contractservice.features.contract.importjob.FailContractImportJob;
import com.mk.contractservice.features.contract.importjob.FinishContractImportJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ContractImportJobRunner {

    public static final int MAX_ATTEMPTS = 5;

    private static final Logger log = LoggerFactory.getLogger(ContractImportJobRunner.class);

    private final Job contractImportJob;
    private final JobOperator jobOperator;
    private final JobRepository jobRepository;
    private final FinishContractImportJob finishContractImportJob;
    private final FailContractImportJob failContractImportJob;

    public void run(final UUID jobId) {
        final JobParameters parameters = new JobParametersBuilder()
                .addString(ContractImportBatchConfiguration.JOB_ID_PARAMETER, jobId.toString())
                .toJobParameters();
        final JobExecution last = jobRepository.getLastJobExecution(ContractImportBatchConfiguration.JOB_NAME,
                parameters);
        if (last != null && last.getStatus() == BatchStatus.COMPLETED) {
            finishContractImportJob.execute(new FinishContractImportJob.Command(jobId));
            return;
        }
        if (last != null && last.isRunning()) {
            jobOperator.recover(last);
        }
        if (failedAttemptsOf(parameters) >= MAX_ATTEMPTS) {
            log.error("Import job {} gave up after {} failed attempts", jobId, MAX_ATTEMPTS);
            fail(jobId, ContractImportFailureReason.TOO_MANY_ATTEMPTS);
            return;
        }
        try {
            jobOperator.start(contractImportJob, parameters);
        } catch (final JobExecutionException ex) {
            log.error("Import job {} could not be started", jobId, ex);
            fail(jobId, ContractImportFailureReason.UNEXPECTED_ERROR);
        }
    }

    private int failedAttemptsOf(final JobParameters parameters) {
        final JobInstance instance = jobRepository.getJobInstance(ContractImportBatchConfiguration.JOB_NAME,
                parameters);
        if (instance == null) {
            return 0;
        }
        return (int) jobRepository.getJobExecutions(instance)
                .stream()
                .filter(execution -> execution.getStatus().isUnsuccessful())
                .count();
    }

    private void fail(final UUID jobId, final ContractImportFailureReason reason) {
        failContractImportJob.execute(new FailContractImportJob.Command(jobId, reason));
    }
}
