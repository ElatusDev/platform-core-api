/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.interfaceadapters;

import com.akademiaplus.security.PasskeyCredentialDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for passkey credential persistence.
 *
 * <p>All queries are automatically filtered by tenant via Hibernate filters
 * on the {@link PasskeyCredentialDataModel} entity.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
public interface PasskeyCredentialJpaRepository
        extends TenantScopedRepository<PasskeyCredentialDataModel, PasskeyCredentialDataModel.PasskeyCredentialCompositeId> {

    /**
     * Finds all credentials for a given user within the tenant.
     *
     * @param userId the user ID
     * @return list of credentials
     */
    List<PasskeyCredentialDataModel> findByUserId(Long userId);

    /**
     * Finds a credential by its authenticator-assigned credential ID within the tenant.
     *
     * @param credentialId the raw credential ID bytes
     * @return the credential, if found
     */
    Optional<PasskeyCredentialDataModel> findByCredentialId(byte[] credentialId);

    /**
     * Finds all credentials by user handle within the tenant.
     *
     * @param userHandle the raw user handle bytes
     * @return list of credentials
     */
    List<PasskeyCredentialDataModel> findByUserHandle(byte[] userHandle);
}
