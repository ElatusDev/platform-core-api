/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.attendance;

import com.akademiaplus.attendance.AttendanceRecordDataModel;
import com.akademiaplus.attendance.StudentType;
import com.akademiaplus.attendance.VerificationMethod;
import com.akademiaplus.util.base.DataFactory;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link AttendanceRecordDataModel} instances with fake data.
 *
 * <p>Requires attendance session IDs and adult student IDs to be injected via
 * setters before {@link #generate(int)} is called.</p>
 */
@Component
public class AttendanceRecordFactory implements DataFactory<AttendanceRecordDataModel> {

    private final ApplicationContext applicationContext;

    @Setter
    private List<Long> availableAttendanceSessionIds = List.of();

    @Setter
    private List<Long> availableAdultStudentIds = List.of();

    /**
     * Constructs the factory with Spring's application context for prototype bean creation.
     *
     * @param applicationContext the Spring application context
     */
    public AttendanceRecordFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<AttendanceRecordDataModel> generate(int count) {
        if (availableAttendanceSessionIds.isEmpty()) {
            throw new IllegalStateException("availableAttendanceSessionIds must be set before generating attendance records");
        }
        if (availableAdultStudentIds.isEmpty()) {
            throw new IllegalStateException("availableAdultStudentIds must be set before generating attendance records");
        }

        List<AttendanceRecordDataModel> records = new ArrayList<>();
        VerificationMethod[] methods = VerificationMethod.values();

        for (int i = 0; i < count; i++) {
            Long sessionId = availableAttendanceSessionIds.get(i % availableAttendanceSessionIds.size());
            Long studentId = availableAdultStudentIds.get(i % availableAdultStudentIds.size());

            AttendanceRecordDataModel model = applicationContext.getBean(AttendanceRecordDataModel.class);
            model.setAttendanceSessionId(sessionId);
            model.setStudentId(studentId);
            model.setStudentType(StudentType.ADULT);
            model.setVerificationMethod(methods[i % methods.length]);
            model.setCheckedInAt(LocalDateTime.now().minusMinutes(30L + i));
            model.setDeviceFingerprint("mock-device-" + (i + 1));
            records.add(model);
        }
        return records;
    }
}
