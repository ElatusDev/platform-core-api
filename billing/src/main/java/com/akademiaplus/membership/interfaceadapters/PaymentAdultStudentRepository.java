/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.interfaceadapters;

import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentAdultStudentRepository extends TenantScopedRepository<PaymentAdultStudentDataModel, PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId> {

    /**
     * Finds payments for a specific adult student by traversing the membership association.
     *
     * @param adultStudentId the adult student ID to filter by
     * @return matching payment records
     */
    @Query("SELECT p FROM PaymentAdultStudentDataModel p "
         + "WHERE p.membershipAdultStudent.adultStudentId = :adultStudentId")
    List<PaymentAdultStudentDataModel> findByAdultStudentId(@Param("adultStudentId") Long adultStudentId);
}
