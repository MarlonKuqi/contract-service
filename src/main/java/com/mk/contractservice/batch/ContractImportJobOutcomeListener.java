package com.mk.contractservice.batch;

import com.mk.contractservice.domain.contract.importjob.ContractImportFailureReason;
import com.mk.contractservice.features.contract.importjob.FailContractImportJob;
import com.mk.contractservice.features.contract.importjob.FinishContractImportJob;
import com.mk.contractservice.features.contract.importjob.ReleaseContractImportJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ContractImportJobOutcomeListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ContractImportJobOutcomeListener.class);

    private final FinishContractImportJob finishContractImportJob;
    private final ReleaseContractImportJob releaseContractImportJob;
    private final FailContractImportJob failContractImportJob;
    private final JobRepository jobRepository;

    @Override
    public void afterJob(final JobExecution jobExecution) {
        final UUID jobId = UUID.fromString(jobExecution.getJobParameters().getString(ContractImportBatchConfiguration.JOB_ID_PARAMETER));
        final JobExecution latest = jobRepository.getLastJobExecution(ContractImportBatchConfiguration.JOB_NAME, jobExecution.getJobParameters());
        if (latest == null || !Objects.equals(latest.getId(), jobExecution.getId())) {
            log.warn("Import job {}: execution {} was replaced by another worker, its outcome {} is ignored", jobId, jobExecution.getId(), jobExecution.getStatus());
            return;
        }
        switch (jobExecution.getStatus()) {
            case COMPLETED -> finishContractImportJob.execute(new FinishContractImportJob.Command(jobId));
            case STOPPED, STOPPING -> {
                log.info("Import job {} stopped, it goes back to the queue", jobId);
                releaseContractImportJob.execute(new ReleaseContractImportJob.Command(jobId));
            }
            default -> onFailure(jobId, jobExecution);
        }
    }

    private void onFailure(final UUID jobId, final JobExecution jobExecution) {
        if (jobExecution.getAllFailureExceptions().stream().anyMatch(ContractImportJobOutcomeListener::isTransient)) {
            log.warn("Import job {} hit a transient error, it goes back to the queue: {}", jobId, jobExecution.getAllFailureExceptions());
            releaseContractImportJob.execute(new ReleaseContractImportJob.Command(jobId));
            return;
        }
        log.error("Import job {} ended with status {}: {}", jobId, jobExecution.getStatus(), jobExecution.getAllFailureExceptions());
        failContractImportJob.execute(new FailContractImportJob.Command(jobId, ContractImportFailureReason.UNEXPECTED_ERROR));
    }

    private static boolean isTransient(final Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof TransientDataAccessException || cause instanceof RecoverableDataAccessException) {
                return true;
            }
        }
        return false;
    }
}
