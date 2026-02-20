/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.courses.event;

import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * Entity representing a course event in the multi-tenant platform.
 * Extends AbstractEvent to inherit common event attributes and adds
 * course-specific relationships including course, collaborator, and attendee tracking.
 * <p>
 * Course events represent specific instances of educational activities
 * with attendance tracking for both adult and minor students.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "course_events")
@SQLDelete(sql = "UPDATE course_events SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND course_event_id = ?")
@IdClass(CourseEventDataModel.CourseEventCompositeId.class)
public class CourseEventDataModel extends AbstractEvent {

    /**
     * Unique identifier for the course event within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "course_event_id")
    private Long courseEventId;

    /**
     * Foreign key to the course this event belongs to.
     * Writable column used to persist the FK value during INSERT.
     */
    @Column(name = "course_id")
    private Long courseId;

    /**
     * Reference to the course this event belongs to.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "course_id", referencedColumnName = "course_id", insertable=false, updatable=false)
    private CourseDataModel course;

    /**
     * Foreign key to the collaborator conducting this event.
     * Writable column used to persist the FK value during INSERT.
     */
    @Column(name = "collaborator_id")
    private Long collaboratorId;

    /**
     * Reference to the collaborator conducting this event.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "collaborator_id", referencedColumnName = "collaborator_id", insertable=false, updatable=false)
    private CollaboratorDataModel collaborator;

    /**
     * List of adult students attending this event.
     * Many-to-many relationship managed through course_event_adult_student_attendees table.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_event_adult_student_attendees",
            joinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false),
                    @JoinColumn(name = "course_event_id", referencedColumnName = "course_event_id", insertable=false, updatable=false)
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false),
                    @JoinColumn(name = "adult_student_id", referencedColumnName = "adult_student_id", insertable=false, updatable=false)
            }
    )
    private List<AdultStudentDataModel> adultAttendees;

    /**
     * List of minor students attending this event.
     * Many-to-many relationship managed through course_event_minor_student_attendees table.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_event_minor_student_attendees",
            joinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false),
                    @JoinColumn(name = "course_event_id", referencedColumnName = "course_event_id", insertable=false, updatable=false)
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false),
                    @JoinColumn(name = "minor_student_id", referencedColumnName = "minor_student_id", insertable=false, updatable=false)
            }
    )
    private List<MinorStudentDataModel> minorAttendees;

    /**
     * Composite primary key class for CourseEvent entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourseEventCompositeId implements Serializable {
        private Long tenantId;
        private Long courseEventId;
    }
}