/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.usecases.ProfileClaimEnricher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Enriches JWT claims with collaborator profile identity.
 *
 * <p>If the authenticated internal user is a collaborator (teacher),
 * adds {@code profile_type = COLLABORATOR} and {@code profile_id = collaboratorId}
 * to the JWT claims map. If the user is not a collaborator (e.g., an employee),
 * the claims map is left unchanged.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class CollaboratorProfileClaimEnricher implements ProfileClaimEnricher {

    private final CollaboratorRepository collaboratorRepository;

    /**
     * Constructs the enricher with the collaborator repository.
     *
     * @param collaboratorRepository the collaborator repository
     */
    public CollaboratorProfileClaimEnricher(CollaboratorRepository collaboratorRepository) {
        this.collaboratorRepository = collaboratorRepository;
    }

    @Override
    public void enrich(Long internalAuthId, Map<String, Object> claims) {
        collaboratorRepository.findByInternalAuthId(internalAuthId)
                .ifPresent(collaborator -> {
                    claims.put(JwtTokenProvider.PROFILE_TYPE_CLAIM, JwtTokenProvider.PROFILE_TYPE_COLLABORATOR);
                    claims.put(JwtTokenProvider.PROFILE_ID_CLAIM, collaborator.getCollaboratorId());
                });
    }
}
