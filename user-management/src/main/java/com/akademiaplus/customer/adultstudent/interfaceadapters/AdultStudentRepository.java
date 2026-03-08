/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdultStudentRepository extends TenantScopedRepository<AdultStudentDataModel, AdultStudentDataModel.AdultStudentCompositeId> {

    /**
     * Finds an adult student by their person PII identifier.
     *
     * @param personPiiId the person PII ID
     * @return the adult student if found
     */
    Optional<AdultStudentDataModel> findByPersonPiiId(Long personPiiId);
}
