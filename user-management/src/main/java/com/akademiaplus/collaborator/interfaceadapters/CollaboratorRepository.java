/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollaboratorRepository extends TenantScopedRepository<CollaboratorDataModel, CollaboratorDataModel.CollaboratorCompositeId> {

    /**
     * Finds a collaborator by their internal authentication ID within the current tenant.
     *
     * @param internalAuthId the internal auth ID from the JWT user_id claim
     * @return an {@link Optional} containing the collaborator, or empty if not found
     */
    Optional<CollaboratorDataModel> findByInternalAuthId(Long internalAuthId);
}
