/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.interfaceadapters;

import com.akademiaplus.security.MagicLinkTokenDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for magic link token persistence.
 *
 * <p>Provides lookup by token hash for magic link verification.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Repository
public interface MagicLinkTokenRepository
        extends TenantScopedRepository<MagicLinkTokenDataModel, MagicLinkTokenDataModel.MagicLinkTokenCompositeId> {

    /**
     * Finds a magic link token by its SHA-256 hash.
     *
     * @param tokenHash the SHA-256 hash of the raw token
     * @return the token entity if found
     */
    Optional<MagicLinkTokenDataModel> findByTokenHash(String tokenHash);
}
