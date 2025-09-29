/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.courses.event;

import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.infra.TenantScoped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Abstract base class for all event types in the multi-tenant platform.
 * Provides common event attributes including date, title, description,
 * and schedule relationship that all concrete event types inherit.
 * <p>
 * Concrete event implementations should extend this class and add
 * their specific attributes and relationships.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractEvent extends TenantScoped {

    /**
     * Date when the event takes place.
     * Used for scheduling and calendar display purposes.
     */
    @Column(name = "event_date", nullable = false, updatable = false)
    private LocalDate eventDate;

    /**
     * Title of the event.
     * Used for display purposes and event identification.
     */
    @Column(name = "event_title", nullable = false, length = 100)
    protected String eventTitle;

    /**
     * Detailed description of the event content and objectives.
     * Used for event information and attendee guidance.
     */
    @Column(name = "event_description", nullable = false, length = 500)
    private String eventDescription;

    /**
     * Reference to the schedule this event follows.
     * Uses composite foreign key to maintain tenant isolation.
     * <p>
     * Links the event to specific course timing and recurrence patterns.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "schedule_id", referencedColumnName = "schedule_id")
    private ScheduleDataModel schedule;

    /**
     * Utility method to check if the event has a title.
     * Used for validation and conditional display logic.
     *
     * @return true if the event has a non-null title
     */
    protected boolean hasTitle() {
        return this.eventTitle != null && !this.eventTitle.trim().isEmpty();
    }
}