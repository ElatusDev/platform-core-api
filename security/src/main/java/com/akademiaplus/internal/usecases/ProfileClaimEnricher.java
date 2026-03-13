/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases;

import java.util.Map;

/**
 * Service provider interface for enriching JWT claims with profile-level
 * identity (e.g., {@code profile_type} and {@code profile_id}).
 *
 * <p>Defined in the {@code security} module so that downstream modules
 * (such as {@code user-management}) can implement it without introducing
 * a circular dependency.</p>
 *
 * <p>The {@link InternalAuthenticationUseCase} invokes all registered
 * enrichers before creating the JWT access token.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@FunctionalInterface
public interface ProfileClaimEnricher {

    /**
     * Enriches the JWT claims map with profile-level identity for the
     * authenticated internal user.
     *
     * <p>Implementations should look up the user's profile entity by
     * {@code internalAuthId} and, if found, add {@code profile_type}
     * and {@code profile_id} claims to the map. If no matching profile
     * exists (e.g., the user is an employee, not a collaborator),
     * the implementation should return without modifying the map.</p>
     *
     * @param internalAuthId the authenticated user's internal auth ID
     * @param claims         the mutable claims map to enrich
     */
    void enrich(Long internalAuthId, Map<String, Object> claims);
}
