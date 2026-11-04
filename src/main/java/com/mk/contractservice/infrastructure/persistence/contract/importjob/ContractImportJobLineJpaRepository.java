package com.mk.contractservice.infrastructure.persistence.contract.importjob;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ContractImportJobLineJpaRepository
        extends JpaRepository<ContractImportJobLineJpaEntity, ContractImportJobLineJpaEntity.LineId> {

    @Query("""
            select line from ContractImportJobLineJpaEntity line
            where line.id.jobId = :jobId and line.rejectionReason is not null
            order by line.id.lineNumber
            """)
    Page<ContractImportJobLineJpaEntity> findRejections(@Param("jobId") UUID jobId, Pageable pageable);
}
