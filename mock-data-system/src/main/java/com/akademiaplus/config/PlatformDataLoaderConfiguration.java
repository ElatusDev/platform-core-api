/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import com.akademiaplus.leadmanagement.interfaceadapters.DemoRequestRepository;
import com.akademiaplus.notification.interfaceadapters.NotificationReadStatusRepository;
import com.akademiaplus.notification.interfaceadapters.PushDeviceRepository;
import com.akademiaplus.notifications.NotificationReadStatusDataModel;
import com.akademiaplus.notifications.PushDeviceDataModel;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.PlatformDataCleanUp;
import com.akademiaplus.util.base.PlatformDataLoader;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Spring configuration for platform-level mock data loader and cleanup beans.
 *
 * <p>Platform-level entities (DemoRequest, NotificationReadStatus, PushDevice)
 * use {@code JpaRepository} with auto-increment {@code Long} keys, so they
 * require {@link PlatformDataLoader} and {@link PlatformDataCleanUp} instead
 * of the tenant-scoped variants.</p>
 */
@Configuration
public class PlatformDataLoaderConfiguration {

    // ── DemoRequest ──

    /**
     * Data loader for demo request entities.
     *
     * @param repository the demo request repository
     * @param factory    the demo request factory
     * @return configured platform data loader
     */
    @Bean
    public PlatformDataLoader<DemoRequestDataModel, DemoRequestDataModel> demoRequestDataLoader(
            DemoRequestRepository repository,
            DataFactory<DemoRequestDataModel> demoRequestFactory) {

        return new PlatformDataLoader<>(repository, Function.identity(), demoRequestFactory);
    }

    /**
     * Data cleanup for demo request entities.
     *
     * @param entityManager JPA entity manager
     * @param repository    the demo request repository
     * @return configured platform data cleanup
     */
    @Bean
    public PlatformDataCleanUp<DemoRequestDataModel> demoRequestDataCleanUp(
            EntityManager entityManager,
            DemoRequestRepository repository) {

        PlatformDataCleanUp<DemoRequestDataModel> cleanup = new PlatformDataCleanUp<>(entityManager);
        cleanup.setDataModel(DemoRequestDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── NotificationReadStatus ──

    /**
     * Data loader for notification read status entities.
     *
     * @param repository the notification read status repository
     * @param factory    the notification read status factory
     * @return configured platform data loader
     */
    @Bean
    public PlatformDataLoader<NotificationReadStatusDataModel, NotificationReadStatusDataModel> notificationReadStatusDataLoader(
            NotificationReadStatusRepository repository,
            DataFactory<NotificationReadStatusDataModel> notificationReadStatusFactory) {

        return new PlatformDataLoader<>(repository, Function.identity(), notificationReadStatusFactory);
    }

    /**
     * Data cleanup for notification read status entities.
     *
     * @param entityManager JPA entity manager
     * @param repository    the notification read status repository
     * @return configured platform data cleanup
     */
    @Bean
    public PlatformDataCleanUp<NotificationReadStatusDataModel> notificationReadStatusDataCleanUp(
            EntityManager entityManager,
            NotificationReadStatusRepository repository) {

        PlatformDataCleanUp<NotificationReadStatusDataModel> cleanup = new PlatformDataCleanUp<>(entityManager);
        cleanup.setDataModel(NotificationReadStatusDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── PushDevice ──

    /**
     * Data loader for push device entities.
     *
     * @param repository the push device repository
     * @param factory    the push device factory
     * @return configured platform data loader
     */
    @Bean
    public PlatformDataLoader<PushDeviceDataModel, PushDeviceDataModel> pushDeviceDataLoader(
            PushDeviceRepository repository,
            DataFactory<PushDeviceDataModel> pushDeviceFactory) {

        return new PlatformDataLoader<>(repository, Function.identity(), pushDeviceFactory);
    }

    /**
     * Data cleanup for push device entities.
     *
     * @param entityManager JPA entity manager
     * @param repository    the push device repository
     * @return configured platform data cleanup
     */
    @Bean
    public PlatformDataCleanUp<PushDeviceDataModel> pushDeviceDataCleanUp(
            EntityManager entityManager,
            PushDeviceRepository repository) {

        PlatformDataCleanUp<PushDeviceDataModel> cleanup = new PlatformDataCleanUp<>(entityManager);
        cleanup.setDataModel(PushDeviceDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }
}
