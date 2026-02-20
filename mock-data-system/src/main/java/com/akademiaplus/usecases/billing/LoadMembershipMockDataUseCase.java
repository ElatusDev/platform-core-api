/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.billing;

import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.billing.dto.MembershipCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock membership records into the database.
 */
@Service
public class LoadMembershipMockDataUseCase
        extends AbstractMockDataUseCase<MembershipCreationRequestDTO, MembershipDataModel, Long> {

    public LoadMembershipMockDataUseCase(
            DataLoader<MembershipCreationRequestDTO, MembershipDataModel, Long> dataLoader,
            @Qualifier("membershipDataCleanUp")
            DataCleanUp<MembershipDataModel, Long> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
