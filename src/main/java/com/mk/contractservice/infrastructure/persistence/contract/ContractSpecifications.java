package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.infrastructure.persistence.contract.entity.ContractJpaEntity;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractSpecifications {

    private static final String FIELD_END_DATE = "endDate";
    private static final String FIELD_CLIENT_ID = "clientId";
    private static final String FIELD_LAST_MODIFIED = "lastModified";

    public static Specification<ContractJpaEntity> isActive() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(FIELD_END_DATE)),
                cb.greaterThan(root.get(FIELD_END_DATE), LocalDateTime.now())
        );
    }

    public static Specification<ContractJpaEntity> hasClientId(final UUID clientId) {
        return (root, query, cb) -> cb.equal(root.get(FIELD_CLIENT_ID), clientId);
    }

    public static Specification<ContractJpaEntity> updatedAfter(final LocalDateTime updatedSince) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(FIELD_LAST_MODIFIED), updatedSince);
    }

    public static class Builder {
        private final List<Specification<ContractJpaEntity>> specifications = new ArrayList<>();

        public Builder active() {
            specifications.add(isActive());
            return this;
        }

        public Builder withClientId(@Nullable final UUID clientId) {
            if (clientId != null) {
                specifications.add(hasClientId(clientId));
            }
            return this;
        }

        public Builder withUpdatedAfter(@Nullable final LocalDateTime updatedSince) {
            if (updatedSince != null) {
                specifications.add(updatedAfter(updatedSince));
            }
            return this;
        }

        public Specification<ContractJpaEntity> build() {
            return (root, query, cb) -> {
                if (specifications.isEmpty()) {
                    return cb.conjunction();
                }

                Predicate[] predicates = specifications.stream()
                        .map(spec -> spec.toPredicate(root, query, cb))
                        .toArray(Predicate[]::new);

                return cb.and(predicates);
            };
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}

