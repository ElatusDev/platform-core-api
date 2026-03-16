/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.course;

/**
 * Lightweight record representing a course-collaborator bridge table row.
 *
 * <p>Used exclusively by {@link CourseAvailableCollaboratorFactory} for mock data generation.
 * This is NOT a JPA entity.</p>
 *
 * @param courseId        the course FK
 * @param collaboratorId  the collaborator FK
 */
public record CourseAvailableCollaboratorRecord(Long courseId, Long collaboratorId) {}
