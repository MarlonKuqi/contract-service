package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.shared.RepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@RepositoryPort
public interface ContractImportJobQueryPort {

    Page<ContractImportRejection> findRejections(UUID jobId, Pageable pageable);
}
