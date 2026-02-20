/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.billing;

import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock membership–adult-student association records into the database.
 */
@Service
public class LoadMembershipAdultStudentMockDataUseCase
        extends AbstractMockDataUseCase<MembershipAdultStudentCreationRequestDTO, MembershipAdultStudentDataModel, Long> {

    public LoadMembershipAdultStudentMockDataUseCase(
            DataLoader<MembershipAdultStudentCreationRequestDTO, MembershipAdultStudentDataModel, Long> dataLoader,
            @Qualifier("membershipAdultStudentDataCleanUp")
            DataCleanUp<MembershipAdultStudentDataModel, Long> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
