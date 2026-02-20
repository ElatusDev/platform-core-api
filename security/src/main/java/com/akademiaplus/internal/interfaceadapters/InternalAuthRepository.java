/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InternalAuthRepository extends TenantScopedRepository<InternalAuthDataModel, InternalAuthDataModel.InternalAuthCompositeId> {
    Optional<InternalAuthDataModel> findByUsernameHash(String usernameHash);

}
