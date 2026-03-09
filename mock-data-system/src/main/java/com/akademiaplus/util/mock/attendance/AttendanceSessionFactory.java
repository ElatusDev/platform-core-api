/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.attendance;

import com.akademiaplus.attendance.AttendanceSessionDataModel;
import com.akademiaplus.attendance.AttendanceSessionStatus;
import com.akademiaplus.util.base.DataFactory;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Factory for creating {@link AttendanceSessionDataModel} instances with fake data.
 *
 * <p>Requires course event IDs to be injected via setter before
 * {@link #generate(int)} is called. The orchestrator wires these IDs
 * through post-load hooks after course events are persisted.</p>
 */
@Component
public class AttendanceSessionFactory implements DataFactory<AttendanceSessionDataModel> {

    private static final int DEFAULT_TOKEN_INTERVAL_SECONDS = 30;

    private final ApplicationContext applicationContext;

    @Setter
    private List<Long> availableCourseEventIds = List.of();

    /**
     * Constructs the factory with Spring's application context for prototype bean creation.
     *
     * @param applicationContext the Spring application context
     */
    public AttendanceSessionFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<AttendanceSessionDataModel> generate(int count) {
        if (availableCourseEventIds.isEmpty()) {
            throw new IllegalStateException("availableCourseEventIds must be set before generating attendance sessions");
        }

        List<AttendanceSessionDataModel> sessions = new ArrayList<>();
        AttendanceSessionStatus[] statuses = AttendanceSessionStatus.values();

        for (int i = 0; i < count; i++) {
            Long courseEventId = availableCourseEventIds.get(i % availableCourseEventIds.size());
            AttendanceSessionStatus status = statuses[i % statuses.length];

            AttendanceSessionDataModel model = applicationContext.getBean(AttendanceSessionDataModel.class);
            model.setCourseEventId(courseEventId);
            model.setStatus(status);
            model.setQrSecret(UUID.randomUUID().toString());
            model.setTokenIntervalSeconds(DEFAULT_TOKEN_INTERVAL_SECONDS);
            model.setStartedAt(LocalDateTime.now().minusHours(2));
            if (status == AttendanceSessionStatus.CLOSED) {
                model.setClosedAt(LocalDateTime.now().minusHours(1));
            }
            sessions.add(model);
        }
        return sessions;
    }
}
