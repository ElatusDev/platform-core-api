/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.courses.program;

import com.akademiaplus.infra.TenantScoped;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
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
 * Entity representing a course in the multi-tenant platform.
 * Defines course information including name, description, capacity limits,
 * and relationships with schedules and available collaborators.
 * <p>
 * Courses serve as the foundation for educational program management,
 * linking students, collaborators, schedules, and events together.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "courses")
@IdClass(CourseDataModel.CourseCompositeId.class)
public class CourseDataModel extends TenantScoped {

    /**
     * Unique identifier for the course within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer courseId;

    /**
     * Name of the course.
     * Used for display purposes and course identification.
     */
    @Column(name = "course_name", nullable = false, length = 100)
    private String courseName;

    /**
     * Detailed description of the course content and objectives.
     * Used for course catalog and enrollment information.
     */
    @Column(name = "course_description", nullable = false, length = 500)
    private String courseDescription;

    /**
     * Maximum number of students that can enroll in this course.
     * Used for enrollment management and capacity planning.
     */
    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    /**
     * List of schedules associated with this course.
     * Defines when and how often the course meets.
     */
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<ScheduleDataModel> schedules;

    /**
     * List of collaborators available to teach this course.
     * Many-to-many relationship managed through course_available_collaborators table.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_available_collaborators",
            joinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id"),
                    @JoinColumn(name = "course_id", referencedColumnName = "course_id")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id"),
                    @JoinColumn(name = "collaborator_id", referencedColumnName = "collaborator_id")
            }
    )
    private List<CollaboratorDataModel> availableCollaborators;

    /**
     * Composite primary key class for Course entity.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CourseCompositeId implements Serializable {
        private Integer tenantId;
        private Integer courseId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CourseCompositeId that)) return false;
            return tenantId.equals(that.tenantId) && courseId.equals(that.courseId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, courseId);
        }

        @Override
        public String toString() {
            return "CourseCompositeId{tenantId=" + tenantId + ", courseId=" + courseId + "}";
        }
    }
}