package com.mk.contractservice.infrastructure.persistence.contract.importjob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContractImportJobFileJpaRepository extends JpaRepository<ContractImportJobFileJpaEntity, UUID> {
}
