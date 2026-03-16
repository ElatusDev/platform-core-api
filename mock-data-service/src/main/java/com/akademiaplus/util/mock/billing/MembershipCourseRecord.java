/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

/**
 * Lightweight record representing a membership-to-course bridge table row.
 *
 * <p>Used exclusively by {@link MembershipCourseFactory} for mock data generation.
 * This is NOT a JPA entity.</p>
 *
 * @param membershipId  the membership FK
 * @param courseId      the course FK
 */
public record MembershipCourseRecord(Long membershipId, Long courseId) {}
