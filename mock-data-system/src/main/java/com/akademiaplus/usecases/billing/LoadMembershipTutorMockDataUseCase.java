/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.billing;

import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock membership–tutor association records into the database.
 */
@Service
public class LoadMembershipTutorMockDataUseCase
        extends AbstractMockDataUseCase<MembershipTutorCreationRequestDTO, MembershipTutorDataModel, MembershipTutorDataModel.MembershipTutorCompositeId> {

    public LoadMembershipTutorMockDataUseCase(
            DataLoader<MembershipTutorCreationRequestDTO, MembershipTutorDataModel, MembershipTutorDataModel.MembershipTutorCompositeId> dataLoader,
            @Qualifier("membershipTutorDataCleanUp")
            DataCleanUp<MembershipTutorDataModel, MembershipTutorDataModel.MembershipTutorCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
