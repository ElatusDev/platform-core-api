/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link MinorStudentDataModel} with tenant-scoped queries.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Repository
public interface MinorStudentRepository extends TenantScopedRepository<MinorStudentDataModel, MinorStudentDataModel.MinorStudentCompositeId> {

    /**
     * Counts active (non-soft-deleted) minor students linked to a specific tutor
     * within a tenant. Used by {@link com.akademiaplus.customer.tutor.usecases.DeleteTutorUseCase}
     * to enforce the pre-delete business rule.
     * <p>
     * The {@code @SQLRestriction("deleted_at IS NULL")} on {@link MinorStudentDataModel}
     * ensures only active students are counted.
     *
     * @param tenantId the tenant ID
     * @param tutorId  the tutor's entity-specific ID
     * @return the count of active minor students for the given tutor
     */
    long countByTenantIdAndTutorId(Long tenantId, Long tutorId);

    /**
     * Finds all minor students linked to a specific tutor.
     *
     * @param tutorId the tutor's entity-specific ID
     * @return list of minor students for this tutor
     */
    List<MinorStudentDataModel> findByTutorId(Long tutorId);
}
