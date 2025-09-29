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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@IdClass(CourseEventDataModel.CourseEventCompositeId.class)
public class CourseEventDataModel extends AbstractEvent {

    /**
     * Unique identifier for the course event within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_event_id")
    private Integer courseEventId;

    /**
     * Reference to the course this event belongs to.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "course_id", referencedColumnName = "course_id")
    private CourseDataModel course;

    /**
     * Reference to the collaborator conducting this event.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "collaborator_id", referencedColumnName = "collaborator_id")
    private CollaboratorDataModel collaborator;

    /**
     * List of adult students attending this event.
     * Many-to-many relationship managed through course_event_adult_student_attendees table.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_event_adult_student_attendees",
            joinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id"),
                    @JoinColumn(name = "course_event_id", referencedColumnName = "course_event_id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id"),
                    @JoinColumn(name = "adult_student_id", referencedColumnName = "adult_student_id")
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
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id"),
                    @JoinColumn(name = "course_event_id", referencedColumnName = "course_event_id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id"),
                    @JoinColumn(name = "minor_student_id", referencedColumnName = "minor_student_id")
            }
    )
    private List<MinorStudentDataModel> minorAttendees;

    /**
     * Assigns a collaborator to this event if the event has a title.
     * Business rule validation to ensure events are properly configured.
     *
     * @param collaborator The collaborator to assign to this event
     */
    public void assignCollaborator(CollaboratorDataModel collaborator) {
        if (this.hasTitle()) {
            this.collaborator = collaborator;
        }
    }

    /**
     * Composite primary key class for CourseEvent entity.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourseEventCompositeId implements Serializable {
        private Integer tenantId;
        private Integer courseEventId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CourseEventCompositeId that)) return false;
            return tenantId.equals(that.tenantId) && courseEventId.equals(that.courseEventId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, courseEventId);
        }

        @Override
        public String toString() {
            return "CourseEventCompositeId{tenantId=" + tenantId + ", courseEventId=" + courseEventId + "}";
        }
    }
}