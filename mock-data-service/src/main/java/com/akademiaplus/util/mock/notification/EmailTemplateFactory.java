/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.notifications.email.EmailTemplateDataModel;
import com.akademiaplus.util.base.DataFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link EmailTemplateDataModel} instances with fake data.
 *
 * <p>No foreign key injection is needed. The {@code templateId} and
 * {@code tenantId} are assigned automatically by the persistence layer.</p>
 */
@Component
public class EmailTemplateFactory implements DataFactory<EmailTemplateDataModel> {

    private final ApplicationContext applicationContext;
    private final EmailTemplateDataGenerator generator;

    /**
     * Constructs the factory with the application context and data generator.
     *
     * @param applicationContext the Spring application context for prototype bean creation
     * @param generator          the email template data generator
     */
    public EmailTemplateFactory(ApplicationContext applicationContext,
                                EmailTemplateDataGenerator generator) {
        this.applicationContext = applicationContext;
        this.generator = generator;
    }

    /**
     * Generates the specified number of {@link EmailTemplateDataModel} instances.
     *
     * @param count the number of email templates to generate
     * @return a list of generated email template data models
     */
    @Override
    public List<EmailTemplateDataModel> generate(int count) {
        List<EmailTemplateDataModel> templates = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            templates.add(createTemplate());
        }
        return templates;
    }

    private EmailTemplateDataModel createTemplate() {
        EmailTemplateDataModel model = applicationContext.getBean(EmailTemplateDataModel.class);
        model.setName(generator.templateName());
        model.setDescription(generator.description());
        model.setCategory(generator.category());
        model.setSubjectTemplate(generator.subjectTemplate());
        model.setBodyHtml(generator.bodyHtml());
        model.setBodyText(generator.bodyText());
        model.setActive(generator.isActive());
        return model;
    }
}
