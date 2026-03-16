/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.notification;

import com.akademiaplus.notifications.email.EmailTemplateDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock email template records into the database.
 */
@Service
public class LoadEmailTemplateMockDataUseCase
        extends AbstractMockDataUseCase<EmailTemplateDataModel, EmailTemplateDataModel, EmailTemplateDataModel.EmailTemplateCompositeId> {

    /**
     * Creates a new use case with the required data loader and cleanup.
     *
     * @param dataLoader  the data loader for email template records
     * @param dataCleanUp the data cleanup for the email_templates table
     */
    public LoadEmailTemplateMockDataUseCase(
            DataLoader<EmailTemplateDataModel, EmailTemplateDataModel, EmailTemplateDataModel.EmailTemplateCompositeId> dataLoader,
            @Qualifier("emailTemplateDataCleanUp")
            DataCleanUp<EmailTemplateDataModel, EmailTemplateDataModel.EmailTemplateCompositeId> dataCleanUp) {
        super(dataLoader, dataCleanUp);
    }
}
