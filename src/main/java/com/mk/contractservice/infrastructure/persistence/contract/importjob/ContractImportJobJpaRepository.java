package com.mk.contractservice.infrastructure.persistence.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJobStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ContractImportJobJpaRepository extends JpaRepository<ContractImportJobJpaEntity, UUID> {

    String SKIP_LOCKED = "-2";

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = SKIP_LOCKED))
    @Query("""
            select job from ContractImportJobJpaEntity job
            where job.status = :pending or (job.status = :running and job.leaseUntil < :now)
            order by job.createdAt
            """)
    List<ContractImportJobJpaEntity> findToRun(
            @Param("pending") ContractImportJobStatus pending,
            @Param("running") ContractImportJobStatus running,
            @Param("now") Instant now,
            Limit limit
    );
}
