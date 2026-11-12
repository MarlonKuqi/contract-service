package com.mk.contractservice.infrastructure.persistence.contract.importjob;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractImportJobJpaRepository extends JpaRepository<ContractImportJobJpaEntity, UUID> {

    String SKIP_LOCKED = "-2";

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = SKIP_LOCKED))
    @Query("""
            select job from ContractImportJobJpaEntity job
            where job.status = com.mk.contractservice.domain.contract.importjob.ContractImportJobState.Status.PENDING
               or (job.status = com.mk.contractservice.domain.contract.importjob.ContractImportJobState.Status.RUNNING
                   and job.leaseUntil < :now)
            order by job.createdAt
            """)
    List<ContractImportJobJpaEntity> findToRun(@Param("now") Instant now, Limit limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from ContractImportJobJpaEntity job where job.id = :id")
    Optional<ContractImportJobJpaEntity> findForUpdate(@Param("id") UUID id);

    Optional<ContractImportJobJpaEntity> findBySubmittedByAndIdempotencyKey(String submittedBy, String idempotencyKey);

    Page<ContractImportJobJpaEntity> findBySubmittedBy(String submittedBy, Pageable pageable);
}
