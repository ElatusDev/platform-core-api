package com.akademiaplus.utilities.idgeneration.interfaceadapters;

import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantSequenceRepository extends JpaRepository<@NonNull TenantSequence, TenantSequence.@NonNull TenantSequenceId> {

    default Optional<TenantSequence> findByTenantIdAndEntityName(Long tenantId, String entityName) {
        return findById(new TenantSequence.TenantSequenceId(tenantId, entityName));
    }
}