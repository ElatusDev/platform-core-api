/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.billing;

import com.akademiaplus.util.base.AbstractBridgeMockDataUseCase;
import com.akademiaplus.util.base.NativeBridgeDataCleanUp;
import com.akademiaplus.util.base.NativeBridgeDataLoader;
import com.akademiaplus.util.mock.billing.MembershipCourseRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock membership-to-course bridge records into the database.
 */
@Service
public class LoadMembershipCourseMockDataUseCase
        extends AbstractBridgeMockDataUseCase<MembershipCourseRecord> {

    /**
     * Creates a new use case with the required data loader and cleanup.
     *
     * @param dataLoader  the native bridge data loader for membership-course records
     * @param dataCleanUp the native bridge data cleanup for the membership_courses table
     */
    public LoadMembershipCourseMockDataUseCase(
            NativeBridgeDataLoader<MembershipCourseRecord> dataLoader,
            @Qualifier("membershipCourseDataCleanUp") NativeBridgeDataCleanUp dataCleanUp) {
        super(dataLoader, dataCleanUp);
    }
}
