/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.interfaceadapters;

import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipAdultStudentRepository
        extends JpaRepository<MembershipAdultStudentDataModel, Long> {
}
