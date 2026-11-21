package com.mk.contractservice.domain.lamal;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import com.mk.contractservice.domain.lamal.exception.LamalExemptionConflictException;
import com.mk.contractservice.domain.lamal.exception.OngoingLamalExemptionException;
import com.mk.contractservice.domain.shared.ContractHolder;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Getter
@EqualsAndHashCode
public class LamalAffiliation {

    private final UUID personId;
    private final List<LamalTerm> terms;

    @Getter(AccessLevel.NONE)
    @Nullable
    private final LamalExemption exemption;

    private LamalAffiliation(
            @Nullable final UUID personId,
            @Nullable final List<LamalTerm> terms,
            @Nullable final LamalExemption exemption
    ) {
        this.personId = notNull(personId);
        this.terms = List.copyOf(notNull(terms));
        this.exemption = exemption;
    }

    public static LamalAffiliation reconstituteFromDatabase(
            @Nullable final UUID personId,
            @Nullable final List<LamalTerm> terms,
            @Nullable final LamalExemption exemption
    ) {
        return new LamalAffiliation(personId, terms, exemption);
    }

    public Optional<LamalExemption> exemption() {
        return Optional.ofNullable(exemption);
    }

    public LamalAffiliation admit(final Contract contract, final LocalDateTime now) {
        ensureAdmits(contract, now);
        final LamalTerm term = LamalTerm.of(contract);
        return new LamalAffiliation(personId, Stream.concat(otherTerms(term), Stream.of(term)).toList(), exemption);
    }

    public void ensureAdmits(final Contract contract, final LocalDateTime now) {
        ensureIsALamalOfThePerson(notNull(contract));
        final LamalTerm term = LamalTerm.of(contract);
        otherTerms(term)
                .filter(other -> other.isActiveAt(notNull(now)))
                .filter(other -> other.overlaps(term))
                .findAny()
                .ifPresent(other -> {
                    throw new OverlappingContractException(personId, ContractType.LAMAL, other.period());
                });
        if (exemption != null && exemption.covers(term.period())) {
            throw LamalExemptionConflictException.forLamal(personId, exemption);
        }
    }

    public LamalAffiliation recordExemption(final LamalExemption newExemption, final LocalDateTime now) {
        notNull(newExemption);
        terms.stream()
                .filter(term -> term.isActiveAt(notNull(now)))
                .filter(term -> newExemption.covers(term.period()))
                .findAny()
                .ifPresent(term -> {
                    throw LamalExemptionConflictException.forExemption(personId, term.period());
                });
        if (exemption != null && exemption.equals(newExemption)) {
            return this;
        }
        return new LamalAffiliation(personId, terms, newExemption);
    }

    public void ensureAcceptsHolder(final ContractHolder holder, final LocalDateTime now) {
        if (exemption != null && notNull(holder).isNotCrossBorder() && exemption.isOngoingAt(notNull(now))) {
            throw new OngoingLamalExemptionException(personId, exemption);
        }
    }

    private Stream<LamalTerm> otherTerms(final LamalTerm term) {
        return terms.stream().filter(other -> !other.isTheSameContractAs(term));
    }

    private void ensureIsALamalOfThePerson(final Contract contract) {
        if (!contract.isLamal() || !personId.equals(contract.clientId())) {
            throw new InvalidDomainObjectError("A LAMal affiliation only holds the LAMal of its person");
        }
    }
}
