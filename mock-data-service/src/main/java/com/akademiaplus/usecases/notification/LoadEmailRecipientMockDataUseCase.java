/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.notification;

import com.akademiaplus.notifications.email.EmailRecipientDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.util.mock.notification.EmailRecipientFactory.EmailRecipientRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock email recipient records into the database.
 */
@Service
public class LoadEmailRecipientMockDataUseCase
        extends AbstractMockDataUseCase<EmailRecipientRequest, EmailRecipientDataModel, EmailRecipientDataModel.EmailRecipientCompositeId> {

    public LoadEmailRecipientMockDataUseCase(
            DataLoader<EmailRecipientRequest, EmailRecipientDataModel, EmailRecipientDataModel.EmailRecipientCompositeId> dataLoader,
            @Qualifier("emailRecipientDataCleanUp")
            DataCleanUp<EmailRecipientDataModel, EmailRecipientDataModel.EmailRecipientCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
