/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.courses.program;

import com.akademiaplus.infra.TenantScoped;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * Entity representing a schedule for courses in the multi-tenant platform.
 * Defines when courses are held including day of week and time periods.
 * <p>
 * Schedules are linked to specific courses and used for planning
 * course events and student attendance tracking.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "schedules")
@IdClass(ScheduleDataModel.ScheduleCompositeId.class)
public class ScheduleDataModel extends TenantScoped {

    /**
     * Unique identifier for the schedule within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Integer scheduleId;

    /**
     * Day of the week when the course is scheduled.
     * Used for recurring course planning and calendar display.
     */
    @Column(name = "schedule_day", nullable = false, length = 9)
    private String scheduleDay;

    /**
     * Start time for the scheduled course session.
     * Defines when the course begins each scheduled day.
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * End time for the scheduled course session.
     * Defines when the course concludes each scheduled day.
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Reference to the course this schedule belongs to.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "course_id", referencedColumnName = "course_id")
    private CourseDataModel course;

    /**
     * Composite primary key class for Schedule entity.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduleCompositeId implements Serializable {
        private Integer tenantId;
        private Integer scheduleId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ScheduleCompositeId that)) return false;
            return tenantId.equals(that.tenantId) && scheduleId.equals(that.scheduleId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, scheduleId);
        }

        @Override
        public String toString() {
            return "ScheduleCompositeId{tenantId=" + tenantId + ", scheduleId=" + scheduleId + "}";
        }
    }
}