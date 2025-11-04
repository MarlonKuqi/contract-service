package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.domain.contract.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ContractJpaRepository extends JpaRepository<Contract, UUID> {

    @Query("""
            SELECT c FROM Contract c
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
            """)
    List<Contract> findActiveContracts(@Param("clientId") UUID clientId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT c FROM Contract c
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
              AND c.lastModified >= :updatedSince
            """)
    List<Contract> findActiveContractsUpdatedAfter(@Param("clientId") final UUID clientId,
                                                   @Param("now") LocalDateTime now,
                                                   @Param("updatedSince") LocalDateTime updatedSince);

    @Modifying
    @Query("""
            UPDATE Contract c
            SET c.period.endDate = :now, c.lastModified = :now
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
            """)
    void closeAllActiveContracts(@Param("clientId") final UUID clientId, @Param("now") final LocalDateTime now);

    @Query("""
            SELECT COALESCE(SUM(c.costAmount.value), 0)
            FROM Contract c
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
            """)
    BigDecimal sumActiveContracts(@Param("clientId") final UUID clientId, @Param("now") final LocalDateTime now);
}