package com.akademiaplus.utilities.idgeneration.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import lombok.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantSequenceRepository extends TenantScopedRepository<@NonNull TenantSequence, TenantSequence.@NonNull TenantSequenceId> {

    default Optional<TenantSequence> findByTenantIdAndEntityName(Long tenantId, String entityName) {
        return findById(new TenantSequence.TenantSequenceId(tenantId, entityName));
    }
}