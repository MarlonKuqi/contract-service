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

/**
 * JPA Specifications centralisées pour les requêtes sur les contrats.
 * Permet de composer dynamiquement des critères de filtrage réutilisables.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractSpecifications {

    // Noms des champs de l'entité ContractJpaEntity
    private static final String FIELD_END_DATE = "endDate";
    private static final String FIELD_CLIENT_ID = "clientId";
    private static final String FIELD_LAST_MODIFIED = "lastModified";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_COST_AMOUNT = "costAmount";

    /**
     * Specification pour filtrer les contrats actifs (sans date de fin ou date de fin future).
     *
     * @return Specification pour les contrats actifs
     */
    public static Specification<ContractJpaEntity> isActive() {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(FIELD_END_DATE)),
                cb.greaterThan(root.get(FIELD_END_DATE), LocalDateTime.now())
        );
    }

    /**
     * Specification pour filtrer les contrats actifs à une date de référence donnée.
     *
     * @param referenceDate Date de référence pour vérifier si le contrat est actif
     * @return Specification pour les contrats actifs à cette date
     */
    public static Specification<ContractJpaEntity> isActiveAt(final LocalDateTime referenceDate) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get(FIELD_END_DATE)),
                cb.greaterThan(root.get(FIELD_END_DATE), referenceDate)
        );
    }

    /**
     * Specification pour filtrer les contrats inactifs (avec date de fin passée).
     *
     * @return Specification pour les contrats inactifs
     */
    public static Specification<ContractJpaEntity> isInactive() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get(FIELD_END_DATE)),
                cb.lessThanOrEqualTo(root.get(FIELD_END_DATE), LocalDateTime.now())
        );
    }

    /**
     * Specification pour filtrer les contrats par client ID.
     *
     * @param clientId ID du client
     * @return Specification pour les contrats de ce client
     */
    public static Specification<ContractJpaEntity> hasClientId(final UUID clientId) {
        return (root, query, cb) -> cb.equal(root.get(FIELD_CLIENT_ID), clientId);
    }

    /**
     * Specification pour filtrer les contrats modifiés après une date donnée.
     *
     * @param updatedSince Date de dernière modification minimale
     * @return Specification pour les contrats modifiés après cette date
     */
    public static Specification<ContractJpaEntity> updatedAfter(final LocalDateTime updatedSince) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(FIELD_LAST_MODIFIED), updatedSince);
    }

    /**
     * Specification pour filtrer les contrats créés après une date donnée.
     *
     * @param createdSince Date de création minimale
     * @return Specification pour les contrats créés après cette date
     */
    public static Specification<ContractJpaEntity> createdAfter(final LocalDateTime createdSince) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(FIELD_CREATED_AT), createdSince);
    }

    /**
     * Builder pour composer dynamiquement des Specifications complexes.
     * Permet de combiner plusieurs critères avec AND.
     */
    public static class Builder {
        private final List<Specification<ContractJpaEntity>> specifications = new ArrayList<>();

        /**
         * Ajoute le critère "contrats actifs".
         *
         * @return Le builder pour chaînage
         */
        public Builder active() {
            specifications.add(isActive());
            return this;
        }

        /**
         * Ajoute le critère "contrats actifs à une date de référence".
         *
         * @param referenceDate Date de référence
         * @return Le builder pour chaînage
         */
        public Builder activeAt(final LocalDateTime referenceDate) {
            specifications.add(isActiveAt(referenceDate));
            return this;
        }

        /**
         * Ajoute le critère "contrats inactifs".
         *
         * @return Le builder pour chaînage
         */
        public Builder inactive() {
            specifications.add(isInactive());
            return this;
        }

        /**
         * Ajoute le critère "par client ID".
         *
         * @param clientId ID du client (peut être null pour ignorer ce critère)
         * @return Le builder pour chaînage
         */
        public Builder withClientId(@Nullable final UUID clientId) {
            if (clientId != null) {
                specifications.add(hasClientId(clientId));
            }
            return this;
        }

        /**
         * Ajoute le critère "modifiés après une date".
         *
         * @param updatedSince Date de dernière modification (peut être null pour ignorer ce critère)
         * @return Le builder pour chaînage
         */
        public Builder updatedAfter(@Nullable final LocalDateTime updatedSince) {
            if (updatedSince != null) {
                specifications.add(ContractSpecifications.updatedAfter(updatedSince));
            }
            return this;
        }

        /**
         * Ajoute le critère "créés après une date".
         *
         * @param createdSince Date de création (peut être null pour ignorer ce critère)
         * @return Le builder pour chaînage
         */
        public Builder createdAfter(@Nullable final LocalDateTime createdSince) {
            if (createdSince != null) {
                specifications.add(ContractSpecifications.createdAfter(createdSince));
            }
            return this;
        }

        /**
         * Ajoute une Specification personnalisée.
         *
         * @param specification Specification à ajouter
         * @return Le builder pour chaînage
         */
        public Builder and(final Specification<ContractJpaEntity> specification) {
            if (specification != null) {
                specifications.add(specification);
            }
            return this;
        }

        /**
         * Construit la Specification finale en combinant tous les critères avec AND.
         *
         * @return La Specification combinée
         */
        public Specification<ContractJpaEntity> build() {
            return (root, query, cb) -> {
                if (specifications.isEmpty()) {
                    return cb.conjunction(); // Toujours vrai si aucun critère
                }

                Predicate[] predicates = specifications.stream()
                        .map(spec -> spec.toPredicate(root, query, cb))
                        .toArray(Predicate[]::new);

                return cb.and(predicates);
            };
        }
    }

    /**
     * Crée un nouveau builder pour composer des Specifications.
     *
     * @return Un nouveau builder
     */
    public static Builder builder() {
        return new Builder();
    }
}

