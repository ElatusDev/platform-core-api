/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.notification;

import com.akademiaplus.notifications.email.EmailDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.util.mock.notification.EmailFactory.EmailRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock email records into the database.
 */
@Service
public class LoadEmailMockDataUseCase
        extends AbstractMockDataUseCase<EmailRequest, EmailDataModel, EmailDataModel.EmailCompositeId> {

    public LoadEmailMockDataUseCase(
            DataLoader<EmailRequest, EmailDataModel, EmailDataModel.EmailCompositeId> dataLoader,
            @Qualifier("emailDataCleanUp")
            DataCleanUp<EmailDataModel, EmailDataModel.EmailCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
