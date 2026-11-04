package com.mk.contractservice.batch;

import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.StepInterruptionPolicy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ContractImportStepInterruptionPolicy implements StepInterruptionPolicy {

    private volatile boolean applicationClosing;

    @EventListener
    void onContextClosed(final ContextClosedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            applicationClosing = true;
        }
    }

    @Override
    public void checkInterrupted(final StepExecution stepExecution) throws JobInterruptedException {
        if (applicationClosing || Thread.currentThread().isInterrupted()) {
            // Without this flag the step ends COMPLETED and skips the remaining lines: https://github.com/spring-projects/spring-batch/issues/5639
            stepExecution.setTerminateOnly();
            throw new JobInterruptedException("Import step stopped at a chunk boundary");
        }
    }
}
