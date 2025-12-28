package com.mk.contractservice.domain.contract.aggregate;

import com.mk.contractservice.domain.contract.exception.ExpiredContractException;
import com.mk.contractservice.domain.contract.exception.InvalidContractException;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import com.mk.contractservice.domain.shared.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class Contract extends Entity {

    @Nullable
    UUID id;
    UUID clientId;
    ContractPeriod period;
    ContractCost costAmount;

    @Builder(toBuilder = true)
    private Contract(
            @Nullable final UUID id,
            @Nullable final UUID clientId,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount
    ) {
        this.id = id;
        this.clientId = clientId;
        this.period = period;
        this.costAmount = costAmount;
    }

    private static UUID guardClientId(@Nullable final UUID clientId) {
        return guardNotNull(clientId, InvalidContractException::forNullClient);
    }

    private static ContractPeriod guardPeriod(@Nullable final ContractPeriod period) {
        return guardNotNull(period, InvalidContractException::forNullPeriod);
    }

    private static ContractCost guardCostAmount(@Nullable final ContractCost costAmount) {
        return guardNotNull(costAmount, InvalidContractException::forNullCostAmount);
    }


    public static Contract of(
            @Nullable final UUID clientId,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount
    ) {
        return builder()
                .clientId(guardClientId(clientId))
                .period(guardPeriod(period))
                .costAmount(guardCostAmount(costAmount))
                .build();
    }

    public static Contract reconstituteFromDatabase(
            @Nullable final UUID id,
            @Nullable final UUID clientId,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount
    ) {
        final Class<Contract> currentClass = Contract.class;
        return builder()
                .id(guardNotNull(id, "id", currentClass))
                .clientId(guardNotNull(clientId, "clientId", currentClass))
                .period(guardNotNull(period, "period", currentClass))
                .costAmount(guardNotNull(costAmount, "costAmount", currentClass))
                .build();
    }

    public boolean isActive() {
        return period.isActive();
    }

    public boolean isInactive() {
        return !isActive();
    }

    public Contract changeCost(final ContractCost newAmount) {
        if (isInactive()) {
            throw new ExpiredContractException(getId());
        }
        return toBuilder()
                .costAmount(guardCostAmount(newAmount))
                .build();
    }

}
