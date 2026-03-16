/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.notification;

import com.akademiaplus.notifications.email.EmailTemplateVariableDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock email template variable records into the database.
 */
@Service
public class LoadEmailTemplateVariableMockDataUseCase
        extends AbstractMockDataUseCase<EmailTemplateVariableDataModel, EmailTemplateVariableDataModel, EmailTemplateVariableDataModel.EmailTemplateVariableCompositeId> {

    /**
     * Creates a new use case with the required data loader and cleanup.
     *
     * @param dataLoader  the data loader for email template variable records
     * @param dataCleanUp the data cleanup for the email_template_variables table
     */
    public LoadEmailTemplateVariableMockDataUseCase(
            DataLoader<EmailTemplateVariableDataModel, EmailTemplateVariableDataModel, EmailTemplateVariableDataModel.EmailTemplateVariableCompositeId> dataLoader,
            @Qualifier("emailTemplateVariableDataCleanUp")
            DataCleanUp<EmailTemplateVariableDataModel, EmailTemplateVariableDataModel.EmailTemplateVariableCompositeId> dataCleanUp) {
        super(dataLoader, dataCleanUp);
    }
}
