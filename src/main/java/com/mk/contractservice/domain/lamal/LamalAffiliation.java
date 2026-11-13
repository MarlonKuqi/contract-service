package com.mk.contractservice.domain.lamal;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Getter
@EqualsAndHashCode
public class LamalAffiliation {

    private final UUID personId;
    private final List<LamalTerm> terms;

    private LamalAffiliation(@Nullable final UUID personId, @Nullable final List<LamalTerm> terms) {
        this.personId = notNull(personId);
        this.terms = List.copyOf(notNull(terms));
    }

    public static LamalAffiliation reconstituteFromDatabase(
            @Nullable final UUID personId,
            @Nullable final List<LamalTerm> terms
    ) {
        return new LamalAffiliation(personId, terms);
    }

    public LamalAffiliation admit(final Contract contract, final LocalDateTime now) {
        ensureIsALamalOfThePerson(notNull(contract));
        final LamalTerm term = LamalTerm.of(contract);
        otherTerms(term)
                .filter(other -> other.isActiveAt(notNull(now)))
                .filter(other -> other.overlaps(term))
                .findAny()
                .ifPresent(other -> {
                    throw new OverlappingContractException(personId, ContractType.LAMAL, other.period());
                });
        return new LamalAffiliation(personId, Stream.concat(otherTerms(term), Stream.of(term)).toList());
    }

    private Stream<LamalTerm> otherTerms(final LamalTerm term) {
        return terms.stream().filter(other -> !other.isTheSameContractAs(term));
    }

    private void ensureIsALamalOfThePerson(final Contract contract) {
        if (contract.type() != ContractType.LAMAL || !personId.equals(contract.clientId())) {
            throw new InvalidDomainObjectError("A LAMal affiliation only holds the LAMal of its person");
        }
    }
}
