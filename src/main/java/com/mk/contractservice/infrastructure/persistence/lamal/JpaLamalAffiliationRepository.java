package com.mk.contractservice.infrastructure.persistence.lamal;

import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.lamal.LamalAffiliation;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.domain.lamal.LamalExemption;
import com.mk.contractservice.domain.lamal.LamalTerm;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaEntity;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaLamalAffiliationRepository implements LamalAffiliationRepository {

    private final LamalAffiliationJpaRepository lamalAffiliationJpaRepository;
    private final ContractJpaRepository contractJpaRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<LamalAffiliation> loadForAdmission(final Set<UUID> clientIds) {
        if (clientIds.isEmpty()) {
            return List.of();
        }
        lamalAffiliationJpaRepository.insertMissingForPersons(clientIds);
        final List<LamalAffiliationJpaEntity> entities = lamalAffiliationJpaRepository
                .findAllByPersonIdInOrderByPersonId(clientIds);
        if (entities.isEmpty()) {
            return List.of();
        }
        final Set<UUID> personIds = entities.stream()
                .map(LamalAffiliationJpaEntity::getPersonId)
                .collect(Collectors.toUnmodifiableSet());
        final Map<UUID, List<LamalTerm>> termsByPersonId = contractJpaRepository
                .findAllByClientIdInAndTypeIn(personIds, Set.of(ContractType.LAMAL))
                .stream()
                .collect(Collectors.groupingBy(entity -> Objects.requireNonNull(entity.getClientId()),
                        Collectors.mapping(JpaLamalAffiliationRepository::toTerm, Collectors.toList())));
        return entities.stream()
                .map(entity -> LamalAffiliation.reconstituteFromDatabase(entity.getPersonId(),
                        termsByPersonId.getOrDefault(entity.getPersonId(), List.of()),
                        toExemption(entity)))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public LamalAffiliation loadForUpdate(final UUID personId) {
        return loadForAdmission(Set.of(personId)).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No person " + personId + " to hold a LAMal affiliation"));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(final LamalAffiliation affiliation) {
        final LamalAffiliationJpaEntity entity = lamalAffiliationJpaRepository.findById(affiliation.personId())
                .orElseThrow(() -> new IllegalStateException(
                        "LAMal affiliation of person " + affiliation.personId() + " not found in database"));
        entity.setExemptionStartDate(affiliation.exemption().map(LamalExemption::startDate).orElse(null));
        entity.setExemptionEndDate(affiliation.exemption().map(LamalExemption::endDate).orElse(null));
    }

    @Override
    public Optional<LamalExemption> findExemption(final UUID personId) {
        return lamalAffiliationJpaRepository.findById(personId)
                .map(JpaLamalAffiliationRepository::toExemption);
    }

    private static @Nullable LamalExemption toExemption(final LamalAffiliationJpaEntity entity) {
        return entity.getExemptionStartDate() == null ? null : LamalExemption.reconstituteFromDatabase(entity
                .getExemptionStartDate(),
                entity.getExemptionEndDate());
    }

    private static LamalTerm toTerm(final ContractJpaEntity entity) {
        return LamalTerm.reconstituteFromDatabase(entity.getId(),
                ContractPeriod.reconstituteFromDatabase(entity.getStartDate(), entity.getEndDate()));
    }
}
