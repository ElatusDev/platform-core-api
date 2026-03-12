/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.leadmanagement;

import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import com.akademiaplus.util.base.DataFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link DemoRequestDataModel} instances with fake data.
 *
 * <p>DemoRequest is a platform-level entity with no FK dependencies.
 * Each generated record has a unique sequential email address.</p>
 */
@Component
public class DemoRequestFactory implements DataFactory<DemoRequestDataModel> {

    private static final String DEFAULT_STATUS = "PENDING";

    private final ApplicationContext applicationContext;

    /**
     * Constructs the factory with Spring's application context for prototype bean creation.
     *
     * @param applicationContext the Spring application context
     */
    public DemoRequestFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<DemoRequestDataModel> generate(int count) {
        List<DemoRequestDataModel> requests = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            DemoRequestDataModel model = applicationContext.getBean(DemoRequestDataModel.class);
            model.setFirstName("Demo-" + (i + 1));
            model.setLastName("Request-" + (i + 1));
            model.setEmail("demo-" + timestamp + "-" + (i + 1) + "@e2e-test.com");
            model.setCompanyName("Test Company " + (i + 1));
            model.setMessage("Demo request message " + (i + 1));
            model.setStatus(DEFAULT_STATUS);
            requests.add(model);
        }
        return requests;
    }
}
