/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.notifications.email.EmailTemplateVariableDataModel;
import com.akademiaplus.util.base.DataFactory;
import lombok.Setter;
import net.datafaker.Faker;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory for creating {@link EmailTemplateVariableDataModel} instances with fake data.
 *
 * <p>Requires template IDs to be injected via setter before
 * {@link #generate(int)} is called. Variable names, types, and required flags
 * cycle through predefined lists for deterministic coverage.</p>
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class EmailTemplateVariableFactory implements DataFactory<EmailTemplateVariableDataModel> {

    /** Error message when template IDs have not been set. */
    public static final String ERROR_TEMPLATE_IDS_NOT_SET =
            "availableTemplateIds must be set before generating email template variables";

    private static final String[] VARIABLE_NAMES = {
            "orgName", "userName", "title", "actionUrl", "amount", "date"
    };

    private static final String[] VARIABLE_TYPES = {
            "STRING", "NUMBER", "DATE", "URL"
    };

    private static final double REQUIRED_PROBABILITY = 0.6;

    private final ApplicationContext applicationContext;
    private final Faker faker;
    private final Random random;
    private final AtomicInteger nameCounter = new AtomicInteger(0);
    private final AtomicInteger typeCounter = new AtomicInteger(0);

    @Setter
    private List<Long> availableTemplateIds = List.of();

    /**
     * Constructs the factory with the application context.
     *
     * @param applicationContext the Spring application context for prototype bean creation
     */
    public EmailTemplateVariableFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.faker = new Faker(Locale.of("es", "MX"));
        this.random = new Random();
    }

    /**
     * Generates the specified number of {@link EmailTemplateVariableDataModel} instances.
     *
     * @param count the number of template variables to generate
     * @return a list of generated template variable data models
     * @throws IllegalStateException if template IDs have not been set
     */
    @Override
    public List<EmailTemplateVariableDataModel> generate(int count) {
        if (availableTemplateIds.isEmpty()) {
            throw new IllegalStateException(ERROR_TEMPLATE_IDS_NOT_SET);
        }

        List<EmailTemplateVariableDataModel> variables = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long templateId = availableTemplateIds.get(i % availableTemplateIds.size());
            variables.add(createVariable(templateId));
        }
        return variables;
    }

    private EmailTemplateVariableDataModel createVariable(Long templateId) {
        EmailTemplateVariableDataModel model =
                applicationContext.getBean(EmailTemplateVariableDataModel.class);

        boolean isRequired = random.nextDouble() < REQUIRED_PROBABILITY;

        model.setTemplateId(templateId);
        model.setName(VARIABLE_NAMES[nameCounter.getAndIncrement() % VARIABLE_NAMES.length]);
        model.setVariableType(VARIABLE_TYPES[typeCounter.getAndIncrement() % VARIABLE_TYPES.length]);
        model.setDescription(faker.lorem().sentence(4));
        model.setRequired(isRequired);
        model.setDefaultValue(resolveDefaultValue(isRequired));
        return model;
    }

    private String resolveDefaultValue(boolean isRequired) {
        if (isRequired) {
            return null;
        }
        return faker.lorem().word();
    }
}
