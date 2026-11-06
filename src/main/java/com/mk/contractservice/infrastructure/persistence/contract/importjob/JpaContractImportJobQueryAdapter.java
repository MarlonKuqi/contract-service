package com.mk.contractservice.infrastructure.persistence.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJobQueryPort;
import com.mk.contractservice.domain.contract.importjob.ContractImportRejection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaContractImportJobQueryAdapter implements ContractImportJobQueryPort {

    private final ContractImportJobLineJpaRepository lineJpaRepository;

    @Override
    public Page<ContractImportRejection> findRejections(final UUID jobId, final Pageable pageable) {
        return lineJpaRepository.findRejections(jobId, pageable)
                .map(ContractImportJobJpaMapper::toDomainRejection);
    }
}
