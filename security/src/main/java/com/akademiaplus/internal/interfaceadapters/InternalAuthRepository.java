/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

import com.akademiaplus.security.InternalAuthDataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InternalAuthRepository extends JpaRepository<InternalAuthDataModel, Long> {
    Optional<InternalAuthDataModel> findByUsernameHash(String usernameHash);

}
