package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

import static com.mk.contractservice.domain.shared.Assert.notNull;

public sealed interface ContractImportJobState permits
        ContractImportJobState.Pending,
        ContractImportJobState.Running,
        ContractImportJobState.Completed,
        ContractImportJobState.Failed,
        ContractImportJobState.Cancelled {

    enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    Status status();

    default @Nullable Instant leaseUntil() {
        return null;
    }

    default @Nullable ContractImportTermination termination() {
        return null;
    }

    default @Nullable ContractImportCancellation cancellation() {
        return null;
    }

    static Pending pending() {
        return new Pending();
    }

    static Running running(@Nullable final Instant leaseUntil) {
        return new Running(leaseUntil);
    }

    static Completed completed(final ContractImportTermination.@Nullable Ended termination) {
        return new Completed(termination);
    }

    static Failed failed(final ContractImportTermination.@Nullable Failed termination) {
        return new Failed(termination);
    }

    static Cancelled cancelled(
            @Nullable final ContractImportCancellation cancellation,
            @Nullable final ContractImportTermination termination
    ) {
        return new Cancelled(cancellation, termination);
    }

    static ContractImportJobState reconstituteFromDatabase(
            @Nullable final Status status,
            @Nullable final Instant leaseUntil,
            @Nullable final ContractImportTermination termination,
            @Nullable final ContractImportCancellation cancellation
    ) {
        final ContractImportJobState state = switch (notNull(status)) {
            case PENDING -> pending();
            case RUNNING -> running(leaseUntil);
            case COMPLETED -> termination instanceof ContractImportTermination.Ended ended
                    ? completed(ended)
                    : inconsistent(status, leaseUntil, termination, cancellation);
            case FAILED -> termination instanceof ContractImportTermination.Failed failed
                    ? failed(failed)
                    : inconsistent(status, leaseUntil, termination, cancellation);
            case CANCELLED -> cancelled(cancellation, termination);
        };
        return isKeepingEveryValue(leaseUntil, termination, cancellation, state) ? state : inconsistent(status, leaseUntil, termination, cancellation);
    }

    private static boolean isKeepingEveryValue(@Nullable Instant leaseUntil, @Nullable ContractImportTermination termination,
                                               @Nullable ContractImportCancellation cancellation, ContractImportJobState state) {
        return Objects.equals(state.leaseUntil(), leaseUntil)
                && Objects.equals(state.termination(), termination)
                && Objects.equals(state.cancellation(), cancellation);
    }


    private static ContractImportJobState inconsistent(
            final Status status,
            @Nullable final Instant leaseUntil,
            @Nullable final ContractImportTermination termination,
            @Nullable final ContractImportCancellation cancellation
    ) {
        throw new InvalidDomainObjectError("An import job cannot be " + status + " with lease " + leaseUntil
                + ", termination " + termination + " and cancellation " + cancellation);
    }

    record Pending() implements ContractImportJobState {

        @Override
        public Status status() {
            return Status.PENDING;
        }
    }

    record Running(Instant leaseUntil) implements ContractImportJobState {

        public Running {
            notNull(leaseUntil);
        }

        @Override
        public Status status() {
            return Status.RUNNING;
        }
    }

    record Completed(ContractImportTermination.Ended termination) implements ContractImportJobState {

        public Completed {
            notNull(termination);
        }

        @Override
        public Status status() {
            return Status.COMPLETED;
        }
    }

    record Failed(ContractImportTermination.Failed termination) implements ContractImportJobState {

        public Failed {
            notNull(termination);
        }

        @Override
        public Status status() {
            return Status.FAILED;
        }
    }

    record Cancelled(ContractImportCancellation cancellation, ContractImportTermination termination)
            implements ContractImportJobState {

        public Cancelled {
            notNull(cancellation);
            notNull(termination);
        }

        @Override
        public Status status() {
            return Status.CANCELLED;
        }
    }
}
