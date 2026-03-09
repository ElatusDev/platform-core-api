/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.attendance.AttendanceRecordDataModel;
import com.akademiaplus.attendance.AttendanceSessionDataModel;
import com.akademiaplus.attendance.interfaceadapters.AttendanceRecordRepository;
import com.akademiaplus.attendance.interfaceadapters.AttendanceSessionRepository;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Spring configuration for attendance-related mock data loader and cleanup beans.
 *
 * <p>Both attendance session and record are tenant-scoped entities using
 * composite keys and {@code TenantScopedRepository}. The factories produce
 * data model instances directly (no creation DTO).</p>
 */
@Configuration
public class AttendanceDataLoaderConfiguration {

    // ── AttendanceSession ──

    /**
     * Data loader for attendance session entities.
     *
     * @param repository the attendance session repository
     * @param factory    the attendance session factory
     * @return configured data loader
     */
    @Bean
    public DataLoader<AttendanceSessionDataModel, AttendanceSessionDataModel, AttendanceSessionDataModel.AttendanceSessionCompositeId> attendanceSessionDataLoader(
            AttendanceSessionRepository repository,
            DataFactory<AttendanceSessionDataModel> attendanceSessionFactory) {

        return new DataLoader<>(repository, Function.identity(), attendanceSessionFactory);
    }

    /**
     * Data cleanup for attendance session entities.
     *
     * @param entityManager JPA entity manager
     * @param repository    the attendance session repository
     * @return configured data cleanup
     */
    @Bean
    public DataCleanUp<AttendanceSessionDataModel, AttendanceSessionDataModel.AttendanceSessionCompositeId> attendanceSessionDataCleanUp(
            EntityManager entityManager,
            AttendanceSessionRepository repository) {

        DataCleanUp<AttendanceSessionDataModel, AttendanceSessionDataModel.AttendanceSessionCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(AttendanceSessionDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── AttendanceRecord ──

    /**
     * Data loader for attendance record entities.
     *
     * @param repository the attendance record repository
     * @param factory    the attendance record factory
     * @return configured data loader
     */
    @Bean
    public DataLoader<AttendanceRecordDataModel, AttendanceRecordDataModel, AttendanceRecordDataModel.AttendanceRecordCompositeId> attendanceRecordDataLoader(
            AttendanceRecordRepository repository,
            DataFactory<AttendanceRecordDataModel> attendanceRecordFactory) {

        return new DataLoader<>(repository, Function.identity(), attendanceRecordFactory);
    }

    /**
     * Data cleanup for attendance record entities.
     *
     * @param entityManager JPA entity manager
     * @param repository    the attendance record repository
     * @return configured data cleanup
     */
    @Bean
    public DataCleanUp<AttendanceRecordDataModel, AttendanceRecordDataModel.AttendanceRecordCompositeId> attendanceRecordDataCleanUp(
            EntityManager entityManager,
            AttendanceRecordRepository repository) {

        DataCleanUp<AttendanceRecordDataModel, AttendanceRecordDataModel.AttendanceRecordCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(AttendanceRecordDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }
}
