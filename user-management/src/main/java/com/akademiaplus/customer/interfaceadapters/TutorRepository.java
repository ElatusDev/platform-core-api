/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.users.customer.TutorDataModel;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorRepository extends TenantScopedRepository<TutorDataModel, TutorDataModel.TutorCompositeId> {

    /**
     * Finds a tutor by their person PII identifier.
     *
     * @param personPiiId the person PII ID
     * @return the tutor if found
     */
    Optional<TutorDataModel> findByPersonPiiId(Long personPiiId);
}
