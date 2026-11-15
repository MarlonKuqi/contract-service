package com.mk.contractservice.domain.lamal;

import com.mk.contractservice.domain.contract.Contract;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LamalAffiliationFixtures {

    public static List<LamalAffiliation> loadedFor(final Collection<UUID> personIds, final List<Contract> contracts) {
        return personIds.stream()
                .map(personId -> loadedFor(personId, contracts, null))
                .toList();
    }

    public static LamalAffiliation loadedFor(final UUID personId,
            final List<Contract> contracts,
            @Nullable final LamalExemption exemption) {
        return LamalAffiliation.reconstituteFromDatabase(personId,
                contracts.stream()
                        .filter(contract -> personId.equals(contract.clientId()))
                        .map(contract -> LamalTerm.reconstituteFromDatabase(contract.id(), contract.period()))
                        .toList(),
                exemption);
    }
}
