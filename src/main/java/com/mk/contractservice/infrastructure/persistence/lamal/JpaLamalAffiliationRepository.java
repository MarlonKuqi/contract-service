package com.mk.contractservice.infrastructure.persistence.lamal;

import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.lamal.LamalAffiliation;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.domain.lamal.LamalTerm;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaEntity;
import com.mk.contractservice.infrastructure.persistence.contract.ContractJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        final Set<UUID> personIds = lamalAffiliationJpaRepository.findAllByPersonIdInOrderByPersonId(clientIds)
                .stream()
                .map(LamalAffiliationJpaEntity::getPersonId)
                .collect(Collectors.toUnmodifiableSet());
        if (personIds.isEmpty()) {
            return List.of();
        }
        final Map<UUID, List<LamalTerm>> termsByPersonId = contractJpaRepository
                .findAllByClientIdInAndTypeIn(personIds, Set.of(ContractType.LAMAL))
                .stream()
                .collect(Collectors.groupingBy(entity -> Objects.requireNonNull(entity.getClientId()),
                        Collectors.mapping(JpaLamalAffiliationRepository::toTerm, Collectors.toList())));
        return personIds.stream()
                .map(personId -> LamalAffiliation.reconstituteFromDatabase(personId,
                        termsByPersonId.getOrDefault(personId, List.of())))
                .toList();
    }

    private static LamalTerm toTerm(final ContractJpaEntity entity) {
        return LamalTerm.reconstituteFromDatabase(entity.getId(),
                ContractPeriod.reconstituteFromDatabase(entity.getStartDate(), entity.getEndDate()));
    }
}
