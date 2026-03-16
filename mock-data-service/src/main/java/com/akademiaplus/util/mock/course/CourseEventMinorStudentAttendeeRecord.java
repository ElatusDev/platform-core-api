/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.course;

/**
 * Lightweight record representing a course-event-to-minor-student attendee bridge table row.
 *
 * <p>Used exclusively by {@link CourseEventMinorStudentAttendeeFactory} for mock data generation.
 * This is NOT a JPA entity.</p>
 *
 * @param courseEventId   the course event FK
 * @param minorStudentId  the minor student FK
 */
public record CourseEventMinorStudentAttendeeRecord(Long courseEventId, Long minorStudentId) {}
