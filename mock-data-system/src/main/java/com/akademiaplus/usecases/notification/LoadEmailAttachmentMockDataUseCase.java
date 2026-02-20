/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.notification;

import com.akademiaplus.notifications.email.EmailAttachmentDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.util.mock.notification.EmailAttachmentFactory.EmailAttachmentRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock email attachment records into the database.
 */
@Service
public class LoadEmailAttachmentMockDataUseCase
        extends AbstractMockDataUseCase<EmailAttachmentRequest, EmailAttachmentDataModel, EmailAttachmentDataModel.EmailAttachmentCompositeId> {

    public LoadEmailAttachmentMockDataUseCase(
            DataLoader<EmailAttachmentRequest, EmailAttachmentDataModel, EmailAttachmentDataModel.EmailAttachmentCompositeId> dataLoader,
            @Qualifier("emailAttachmentDataCleanUp")
            DataCleanUp<EmailAttachmentDataModel, EmailAttachmentDataModel.EmailAttachmentCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
