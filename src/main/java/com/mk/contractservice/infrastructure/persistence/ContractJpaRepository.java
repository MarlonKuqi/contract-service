package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.domain.contract.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ContractJpaRepository extends JpaRepository<Contract, UUID> {

    @Query("""
            SELECT c FROM Contract c
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
            """)
    Page<Contract> findActiveContractsPageable(@Param("clientId") UUID clientId,
                                               @Param("now") LocalDateTime now,
                                               Pageable pageable);


    @Query("""
            SELECT c FROM Contract c
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
              AND c.lastModified >= :updatedSince
            """)
    Page<Contract> findActiveContractsUpdatedAfterPageable(@Param("clientId") UUID clientId,
                                                           @Param("now") LocalDateTime now,
                                                           @Param("updatedSince") LocalDateTime updatedSince,
                                                           Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Contract c
            SET c.period.endDate = :now, c.lastModified = :now
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
            """)
    void closeAllActiveContracts(@Param("clientId") UUID clientId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT COALESCE(SUM(c.costAmount.value), 0)
            FROM Contract c
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
            """)
    BigDecimal sumActiveContracts(@Param("clientId") UUID clientId, @Param("now") LocalDateTime now);
}
