package com.mk.contractservice.domain.lamal;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractPeriod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LamalTerm {

    @Nullable
    UUID contractId;

    ContractPeriod period;

    public static LamalTerm reconstituteFromDatabase(@Nullable final UUID contractId,
            @Nullable final ContractPeriod period) {
        return new LamalTerm(notNull(contractId), notNull(period));
    }

    static LamalTerm of(final Contract contract) {
        return new LamalTerm(contract.id(), contract.period());
    }

    boolean isActiveAt(final LocalDateTime now) {
        return period.isEffectiveAt(now);
    }

    boolean overlaps(final LamalTerm other) {
        return period.overlaps(other.period);
    }

    boolean isTheSameContractAs(final LamalTerm other) {
        return contractId != null && contractId.equals(other.contractId);
    }
}
