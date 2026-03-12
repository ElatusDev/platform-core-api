/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.interfaceadapters;

import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipAdultStudentRepository
        extends TenantScopedRepository<MembershipAdultStudentDataModel, MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId> {

    /**
     * Finds all membership associations for a specific adult student.
     *
     * @param adultStudentId the adult student ID
     * @return list of membership associations
     */
    List<MembershipAdultStudentDataModel> findByAdultStudentId(Long adultStudentId);
}
