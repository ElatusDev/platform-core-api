/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.billing;

import com.akademiaplus.billing.customerpayment.PaymentTutorDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock tutor payment records into the database.
 */
@Service
public class LoadPaymentTutorMockDataUseCase
        extends AbstractMockDataUseCase<PaymentTutorCreationRequestDTO, PaymentTutorDataModel, PaymentTutorDataModel.PaymentTutorCompositeId> {

    public LoadPaymentTutorMockDataUseCase(
            DataLoader<PaymentTutorCreationRequestDTO, PaymentTutorDataModel, PaymentTutorDataModel.PaymentTutorCompositeId> dataLoader,
            @Qualifier("paymentTutorDataCleanUp")
            DataCleanUp<PaymentTutorDataModel, PaymentTutorDataModel.PaymentTutorCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
