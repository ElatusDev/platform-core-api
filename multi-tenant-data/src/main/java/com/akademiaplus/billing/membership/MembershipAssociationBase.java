/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.membership;

import com.akademiaplus.infra.TenantScoped;
import com.akademiaplus.courses.program.CourseDataModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Abstract base class for membership associations in the multi-tenant platform.
 * Provides common membership relationship attributes including dates, membership reference,
 * and optional course association that concrete membership association implementations inherit.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class MembershipAssociationBase extends TenantScoped {

    /**
     * Date when the membership association starts.
     * Defines the beginning of the membership period.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Date when the membership association is due or expires.
     * Used for payment scheduling and membership renewal tracking.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Reference to the membership type for this association.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "membership_id", referencedColumnName = "membership_id", insertable=false, updatable=false)
    protected MembershipDataModel membership;

    /**
     * Optional reference to a specific course for this membership.
     * Uses composite foreign key to maintain tenant isolation.
     * Null indicates the membership applies to all courses or is course-independent.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "course_id", referencedColumnName = "course_id", insertable=false, updatable=false)
    private CourseDataModel course;
}