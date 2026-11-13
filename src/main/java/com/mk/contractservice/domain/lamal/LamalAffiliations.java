package com.mk.contractservice.domain.lamal;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

public final class LamalAffiliations {

    private final Map<UUID, LamalAffiliation> byPersonId = new HashMap<>();

    private LamalAffiliations(final Collection<LamalAffiliation> affiliations) {
        notNull(affiliations).forEach(affiliation -> byPersonId.put(affiliation.personId(), affiliation));
    }

    public static LamalAffiliations of(final Collection<LamalAffiliation> affiliations) {
        return new LamalAffiliations(affiliations);
    }

    public void admit(final Contract contract, final LocalDateTime now) {
        if (notNull(contract).type() != ContractType.LAMAL) {
            return;
        }
        final UUID personId = Objects.requireNonNull(contract.clientId());
        final LamalAffiliation affiliation = byPersonId.get(personId);
        if (affiliation == null) {
            throw new IllegalStateException("No LAMal affiliation loaded for person " + personId);
        }
        byPersonId.put(personId, affiliation.admit(contract, now));
    }
}
