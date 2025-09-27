/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.membership;

import com.akademiaplus.courses.program.CourseDataModel;
import jakarta.persistence.*;

import java.time.LocalDate;


@MappedSuperclass
public abstract class MembershipAssociationBase {

    @Column(name = "start_date", columnDefinition = "DATE", nullable = false)
    private LocalDate startDate;

    @Column(name = "due_date", columnDefinition = "DATE", nullable = false)
    private LocalDate dueDate;

    @ManyToOne
    @JoinColumn(name = "membership_id")
    protected MembershipDataModel membership;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private CourseDataModel course;
}
