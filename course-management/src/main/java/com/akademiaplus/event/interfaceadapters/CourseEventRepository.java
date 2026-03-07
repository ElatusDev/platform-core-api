/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.interfaceadapters;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseEventRepository extends TenantScopedRepository<CourseEventDataModel, CourseEventDataModel.CourseEventCompositeId> {

    /**
     * Finds course events where the given attendee ID is present in either
     * the adult attendee or minor attendee collections.
     *
     * @param attendeeId the attendee ID to filter by
     * @return matching course events
     */
    @Query("SELECT DISTINCT ce FROM CourseEventDataModel ce "
         + "LEFT JOIN ce.adultAttendees aa "
         + "LEFT JOIN ce.minorAttendees ma "
         + "WHERE aa.adultStudentId = :attendeeId OR ma.minorStudentId = :attendeeId")
    List<CourseEventDataModel> findByAttendeeId(@Param("attendeeId") Long attendeeId);
}
