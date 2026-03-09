/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.notifications.PushDeviceDataModel;
import com.akademiaplus.util.base.DataFactory;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link PushDeviceDataModel} instances with fake data.
 *
 * <p>Requires user IDs to be injected via setter before {@link #generate(int)}
 * is called. Each generated record has a unique sequential device token.</p>
 */
@Component
public class PushDeviceFactory implements DataFactory<PushDeviceDataModel> {

    private static final String DEFAULT_APP_VERSION = "1.0.0";

    private final ApplicationContext applicationContext;

    @Setter
    private List<Long> availableUserIds = List.of();

    /**
     * Constructs the factory with Spring's application context for prototype bean creation.
     *
     * @param applicationContext the Spring application context
     */
    public PushDeviceFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<PushDeviceDataModel> generate(int count) {
        if (availableUserIds.isEmpty()) {
            throw new IllegalStateException("availableUserIds must be set before generating push devices");
        }

        String[] platforms = {"IOS", "ANDROID"};
        List<PushDeviceDataModel> devices = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            Long userId = availableUserIds.get(i % availableUserIds.size());

            PushDeviceDataModel model = applicationContext.getBean(PushDeviceDataModel.class);
            model.setUserId(userId);
            model.setDeviceToken("mock-device-token-" + timestamp + "-" + (i + 1));
            model.setPlatform(platforms[i % platforms.length]);
            model.setAppVersion(DEFAULT_APP_VERSION);
            devices.add(model);
        }
        return devices;
    }
}
