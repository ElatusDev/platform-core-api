/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.course;

/**
 * Lightweight record representing a course-event-to-adult-student attendee bridge table row.
 *
 * <p>Used exclusively by {@link CourseEventAdultStudentAttendeeFactory} for mock data generation.
 * This is NOT a JPA entity.</p>
 *
 * @param courseEventId   the course event FK
 * @param adultStudentId  the adult student FK
 */
public record CourseEventAdultStudentAttendeeRecord(Long courseEventId, Long adultStudentId) {}
