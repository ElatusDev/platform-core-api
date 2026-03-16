/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.course;

/**
 * Lightweight record representing an adult-student-to-course bridge table row.
 *
 * <p>Used exclusively by {@link AdultStudentCourseFactory} for mock data generation.
 * This is NOT a JPA entity.</p>
 *
 * @param adultStudentId  the adult student FK
 * @param courseId        the course FK
 */
public record AdultStudentCourseRecord(Long adultStudentId, Long courseId) {}
