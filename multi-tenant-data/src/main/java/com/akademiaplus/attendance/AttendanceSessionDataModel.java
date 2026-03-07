/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.attendance;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.infra.persistence.model.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing an attendance session linked to a course event.
 * <p>
 * Each session holds the HMAC secret used to generate rotating QR tokens
 * and tracks the session lifecycle from ACTIVE to CLOSED.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "attendance_sessions")
@SQLDelete(sql = "UPDATE attendance_sessions SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND attendance_session_id = ?")
@IdClass(AttendanceSessionDataModel.AttendanceSessionCompositeId.class)
public class AttendanceSessionDataModel extends TenantScoped {

    @Id
    @Column(name = "attendance_session_id")
    private Long attendanceSessionId;

    @Column(name = "course_event_id")
    private Long courseEventId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable = false, updatable = false)
    @JoinColumn(name = "course_event_id", referencedColumnName = "course_event_id", insertable = false, updatable = false)
    private CourseEventDataModel courseEvent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceSessionStatus status;

    @Column(name = "qr_secret", nullable = false, length = 512)
    private String qrSecret;

    @Column(name = "token_interval_seconds", nullable = false)
    private Integer tokenIntervalSeconds;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /**
     * Composite primary key class for AttendanceSession entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttendanceSessionCompositeId implements Serializable {
        private Long tenantId;
        private Long attendanceSessionId;
    }
}
