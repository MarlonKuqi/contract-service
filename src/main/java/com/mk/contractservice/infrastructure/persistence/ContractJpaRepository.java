package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.domain.contract.Contract;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractJpaRepository extends JpaRepository<Contract, UUID> {

    @Query("""
            SELECT c FROM Contract c
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
            """)
    List<Contract> findActiveContracts(@Param("clientId") final UUID clientId, @Param("now") final OffsetDateTime now);

    @Query("""
            SELECT c FROM Contract c
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
              AND c.lastModified >= :updatedSince
            """)
    List<Contract> findActiveContractsUpdatedAfter(@Param("clientId") final UUID clientId,
                                                   @Param("now") OffsetDateTime now,
                                                   @Param("updatedSince") OffsetDateTime updatedSince);

    @Modifying
    @Query("""
            UPDATE Contract c
            SET c.period.endDate = :now, c.lastModified = :now
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
            """)
    void closeAllActiveContracts(@Param("clientId") final UUID clientId, @Param("now") final OffsetDateTime now);

    @Query("""
            SELECT COALESCE(SUM(c.costAmount.value), 0)
            FROM Contract c
            WHERE c.client.id = :clientId
              AND (c.period.endDate IS NULL OR c.period.endDate > :now)
            """)
    BigDecimal sumActiveContracts(@Param("clientId") final UUID clientId, @Param("now") final OffsetDateTime now);
}