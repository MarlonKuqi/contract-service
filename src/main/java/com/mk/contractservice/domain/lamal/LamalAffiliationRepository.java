package com.mk.contractservice.domain.lamal;

import com.mk.contractservice.domain.shared.RepositoryPort;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RepositoryPort
public interface LamalAffiliationRepository {

    List<LamalAffiliation> loadForAdmission(Set<UUID> clientIds);
}
