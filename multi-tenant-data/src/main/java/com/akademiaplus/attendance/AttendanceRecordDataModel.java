/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.attendance;

import com.akademiaplus.infra.persistence.model.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing a single student check-in within an attendance session.
 * <p>
 * Records the verification method used, the student identity,
 * and optional device fingerprint for audit purposes.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "attendance_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_session_student",
                columnNames = {"tenant_id", "attendance_session_id", "student_id", "student_type", "deleted_at"}))
@SQLDelete(sql = "UPDATE attendance_records SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND attendance_record_id = ?")
@IdClass(AttendanceRecordDataModel.AttendanceRecordCompositeId.class)
public class AttendanceRecordDataModel extends TenantScoped {

    @Id
    @Column(name = "attendance_record_id")
    private Long attendanceRecordId;

    @Column(name = "attendance_session_id")
    private Long attendanceSessionId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable = false, updatable = false)
    @JoinColumn(name = "attendance_session_id", referencedColumnName = "attendance_session_id", insertable = false, updatable = false)
    private AttendanceSessionDataModel attendanceSession;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_type", nullable = false, length = 20)
    private StudentType studentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", nullable = false, length = 30)
    private VerificationMethod verificationMethod;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    /**
     * Composite primary key class for AttendanceRecord entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttendanceRecordCompositeId implements Serializable {
        private Long tenantId;
        private Long attendanceRecordId;
    }
}
