/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

import com.akademiaplus.security.RefreshTokenDataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link RefreshTokenDataModel}.
 *
 * <p>Provides lookup by token hash (unique, cross-tenant) and bulk
 * revocation by family ID or user+tenant combination.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Repository
public interface RefreshTokenRepository
        extends JpaRepository<RefreshTokenDataModel, RefreshTokenDataModel.RefreshTokenCompositeId> {

    /**
     * Finds a refresh token by its SHA-256 hash.
     *
     * @param tokenHash the SHA-256 hash of the raw token
     * @return the refresh token if found
     */
    Optional<RefreshTokenDataModel> findByTokenHash(String tokenHash);

    /**
     * Revokes all active tokens in a rotation family by setting their revoked timestamp.
     *
     * @param familyId  the family UUID grouping the rotation chain
     * @param revokedAt the revocation timestamp
     */
    @Modifying
    @Query("UPDATE RefreshTokenDataModel r SET r.revokedAt = :revokedAt "
            + "WHERE r.familyId = :familyId AND r.revokedAt IS NULL")
    void revokeAllByFamilyId(@Param("familyId") String familyId,
                             @Param("revokedAt") Instant revokedAt);

    /**
     * Revokes all active tokens for a given user and tenant.
     *
     * @param userId    the user ID
     * @param tenantId  the tenant ID
     * @param revokedAt the revocation timestamp
     */
    @Modifying
    @Query("UPDATE RefreshTokenDataModel r SET r.revokedAt = :revokedAt "
            + "WHERE r.userId = :userId AND r.tenantId = :tenantId AND r.revokedAt IS NULL")
    void revokeAllByUserIdAndTenantId(@Param("userId") Long userId,
                                      @Param("tenantId") Long tenantId,
                                      @Param("revokedAt") Instant revokedAt);
}
