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

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "course_event")
public class CourseEventDataModel extends AbstractEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_event_id")
    private Integer courseEventId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private CourseDataModel course;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "collaborator_id")
    private CollaboratorDataModel collaborator;

    @ManyToMany
    @JoinTable(
            name = "course_event_adult_student_attendees",
            joinColumns = @JoinColumn(name = "course_event_id"),
            inverseJoinColumns = @JoinColumn(name = "adult_student_id")
    )
    private List<AdultStudentDataModel> adultAttendees;
    @ManyToMany
    @JoinTable(
            name = "course_event_minor_student_attendees",
            joinColumns = @JoinColumn(name = "course_event_id"),
            inverseJoinColumns = @JoinColumn(name = "minor_student_id")
    )
    private List<MinorStudentDataModel> minorAttendees;

    @Override
    protected boolean hasTitle() {
        boolean hasNoTitle = this.title.isEmpty();
        return !hasNoTitle;
    }

    public void addCollaborator(CollaboratorDataModel collaborator) {
        if(this.hasTitle()) {
            this.collaborator = collaborator;
        }
    }
}
