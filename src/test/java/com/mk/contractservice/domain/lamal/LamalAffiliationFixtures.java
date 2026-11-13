package com.mk.contractservice.domain.lamal;

import com.mk.contractservice.domain.contract.Contract;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LamalAffiliationFixtures {

    public static List<LamalAffiliation> loadedFor(final Collection<UUID> personIds, final List<Contract> contracts) {
        return personIds.stream()
                .map(personId -> LamalAffiliation.reconstituteFromDatabase(personId,
                        contracts.stream()
                                .filter(contract -> personId.equals(contract.clientId()))
                                .map(contract -> LamalTerm.reconstituteFromDatabase(contract.id(),
                                        contract.period()))
                                .toList()))
                .toList();
    }
}
