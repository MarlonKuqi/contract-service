package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.exception.ContractTypeNotAllowedException;
import com.mk.contractservice.domain.contract.exception.InvalidContractFranchiseException;
import com.mk.contractservice.domain.shared.PolicyHolder;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

import static com.mk.contractservice.domain.shared.Assert.notNull;

public sealed interface ContractCoverage permits ContractCoverage.Lamal, ContractCoverage.Laa, ContractCoverage.Lpp {

    ContractType type();

    static ContractCoverage of(@Nullable final ContractType type, @Nullable final Integer franchise) {
        final ContractType contractType = notNull(type);
        if (franchise != null && contractType.isNotLAMAL()) {
            throw new InvalidContractFranchiseException("A " + contractType + " contract has no franchise");
        }
        return switch (contractType) {
            case LAMAL -> new Lamal(LamalFranchise.of(franchise));
            case LAA -> new Laa();
            case LPP -> new Lpp();
        };
    }

    static ContractCoverage reconstituteFromDatabase(
            @Nullable final ContractType type,
            @Nullable final Integer franchise
    ) {
        return switch (notNull(type)) {
            case LAMAL -> new Lamal(LamalFranchise.reconstituteFromDatabase(notNull(franchise)));
            case LAA -> new Laa();
            case LPP -> new Lpp();
        };
    }

    default void ensureAllowedFor(final PolicyHolder holder, final ContractPeriod period) {
        if (type().isClosedTo(holder.kind())) {
            throw new ContractTypeNotAllowedException(type(), holder.kind());
        }
        if (this instanceof Lamal(LamalFranchise franchise) && holder instanceof PolicyHolder.Person(
                LocalDate birthDate
        )) {
            franchise.ensureSuits(birthDate, period);
        }
    }

    record Lamal(LamalFranchise franchise) implements ContractCoverage {

        public Lamal {
            notNull(franchise);
        }

        @Override
        public ContractType type() {
            return ContractType.LAMAL;
        }
    }

    record Laa() implements ContractCoverage {

        @Override
        public ContractType type() {
            return ContractType.LAA;
        }
    }

    record Lpp() implements ContractCoverage {

        @Override
        public ContractType type() {
            return ContractType.LPP;
        }
    }
}
