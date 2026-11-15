package com.mk.contractservice.domain.lamal;

import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.lamal.exception.InvalidLamalExemptionPeriodException;
import com.mk.contractservice.domain.lamal.exception.LamalExemptionNotAllowedException;
import com.mk.contractservice.domain.shared.ContractHolder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LamalExemption {

    LocalDate startDate;

    @Nullable
    LocalDate endDate;

    public static LamalExemption of(
            @Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate,
            final ContractHolder holder
    ) {
        if (startDate == null) {
            throw new InvalidLamalExemptionPeriodException("A LAMal exemption must have a start date");
        }
        if (endDate != null && !endDate.isAfter(startDate)) {
            throw new InvalidLamalExemptionPeriodException(
                    "A LAMal exemption must end after it starts. Start: " + startDate + ", End: " + endDate);
        }
        if (notNull(holder).isNotCrossBorder()) {
            throw new LamalExemptionNotAllowedException();
        }
        return new LamalExemption(startDate, endDate);
    }

    public static LamalExemption reconstituteFromDatabase(
            @Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate
    ) {
        return new LamalExemption(notNull(startDate), endDate);
    }

    public String describe() {
        return startDate + " to " + (endDate == null ? "no end date" : endDate.toString());
    }

    boolean isOngoingAt(final LocalDateTime now) {
        return period().isEffectiveAt(now);
    }

    boolean covers(final ContractPeriod contractPeriod) {
        return period().overlaps(contractPeriod);
    }

    private ContractPeriod period() {
        return ContractPeriod.reconstituteFromDatabase(startDate.atStartOfDay(),
                endDate == null ? null : endDate.atStartOfDay());
    }
}
