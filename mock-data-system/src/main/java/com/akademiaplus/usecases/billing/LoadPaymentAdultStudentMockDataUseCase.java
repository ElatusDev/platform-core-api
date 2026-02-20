/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.billing;

import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock adult-student payment records into the database.
 */
@Service
public class LoadPaymentAdultStudentMockDataUseCase
        extends AbstractMockDataUseCase<PaymentAdultStudentCreationRequestDTO, PaymentAdultStudentDataModel, Long> {

    public LoadPaymentAdultStudentMockDataUseCase(
            DataLoader<PaymentAdultStudentCreationRequestDTO, PaymentAdultStudentDataModel, Long> dataLoader,
            @Qualifier("paymentAdultStudentDataCleanUp")
            DataCleanUp<PaymentAdultStudentDataModel, Long> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
