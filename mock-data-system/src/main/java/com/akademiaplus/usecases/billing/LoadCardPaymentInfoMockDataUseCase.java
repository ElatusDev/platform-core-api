/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.billing;

import com.akademiaplus.billing.customerpayment.CardPaymentInfoDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.util.mock.billing.CardPaymentInfoFactory.CardPaymentInfoRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock card payment info records into the database.
 */
@Service
public class LoadCardPaymentInfoMockDataUseCase
        extends AbstractMockDataUseCase<CardPaymentInfoRequest, CardPaymentInfoDataModel, Long> {

    public LoadCardPaymentInfoMockDataUseCase(
            DataLoader<CardPaymentInfoRequest, CardPaymentInfoDataModel, Long> dataLoader,
            @Qualifier("cardPaymentInfoDataCleanUp")
            DataCleanUp<CardPaymentInfoDataModel, Long> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
