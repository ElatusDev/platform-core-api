/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.courses.event;

import com.akademiaplus.courses.program.ScheduleDataModel;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

import java.time.LocalDate;

@MappedSuperclass
public abstract class AbstractEvent {

    @Column(name = "event_date", nullable = false, columnDefinition = "DATE", updatable = false)
    private LocalDate date;
    @Column(name = "event_title", nullable = false, length = 50)
    protected String title;
    @Column(name = "event_description", nullable = false, length = 200)
    private String description;
    @OneToOne
    @JoinColumn(name = "schedule_id")
    private ScheduleDataModel schedule;

    protected boolean hasTitle() {
        return this.title != null;
    }

}
