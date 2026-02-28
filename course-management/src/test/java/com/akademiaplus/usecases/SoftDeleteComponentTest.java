/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tier 3 component test for soft-delete behavior in the course-management module.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and exercises the course delete endpoint end-to-end. Verifies:
 * <ul>
 *   <li>Step 7.3: Soft-delete of a parent course with FK schedule references
 *       succeeds (UPDATE via {@code @SQLDelete}, not physical DELETE), so
 *       foreign key constraints are not violated</li>
 *   <li>Child schedules remain active after parent course is soft-deleted
 *       (soft-delete does not cascade)</li>
 * </ul>
 *
 * @author ElatusDev
 * @since 1.0
 * @see AbstractIntegrationTest
 */
@AutoConfigureMockMvc
@Sql(scripts = "/insert-soft-delete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DisplayName("Soft-Delete — Course Management Component Test")
class SoftDeleteComponentTest extends AbstractIntegrationTest {

    private static final Long TENANT_ID = 1L;

    private static final String COURSES_PATH = "/v1/course-management/courses";

    private static final Long COURSE_ID_WITH_SCHEDULES = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    // ── Step 7.3: Soft-Delete With FK References ─────────────────────────

    @Test
    @DisplayName("Step 7.3 — Should return 204 when soft-deleting course with active schedules")
    void shouldReturn204_whenSoftDeletingCourseWithActiveSchedules() throws Exception {
        // Given — course has child schedules referencing it (FK constraint)
        long scheduleCount = nativeCountWhere(
                "schedules", "course_id = " + COURSE_ID_WITH_SCHEDULES
                        + " AND tenant_id = " + TENANT_ID + " AND deleted_at IS NULL");
        assertThat(scheduleCount)
                .as("course should have at least one active schedule")
                .isGreaterThan(0);

        tenantContextHolder.setTenantId(TENANT_ID);

        // When — soft-delete the course (UPDATE, not physical DELETE)
        mockMvc.perform(delete(COURSES_PATH + "/{courseId}", COURSE_ID_WITH_SCHEDULES))
                .andExpect(status().isNoContent());

        entityManager.clear();

        // Then — course is soft-deleted, schedules remain active (not cascaded)
        assertThat(nativeDeletedAt("courses", "course_id", COURSE_ID_WITH_SCHEDULES))
                .as("course should have deleted_at set")
                .isNotNull();
        long remainingSchedules = nativeCountWhere(
                "schedules", "course_id = " + COURSE_ID_WITH_SCHEDULES
                        + " AND tenant_id = " + TENANT_ID + " AND deleted_at IS NULL");
        assertThat(remainingSchedules)
                .as("child schedules should remain active (soft-delete does not cascade)")
                .isEqualTo(scheduleCount);
    }

    // ── Helper Methods ───────────────────────────────────────────────────

    /**
     * Returns the {@code deleted_at} value for a specific entity row.
     * Uses native SQL to bypass {@code @SQLRestriction}.
     *
     * @param tableName the database table
     * @param idColumn  the entity ID column
     * @param entityId  the entity-specific ID
     * @return the deleted_at timestamp, or null if not soft-deleted
     */
    private Object nativeDeletedAt(String tableName, String idColumn, Long entityId) {
        return entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM " + tableName
                                + " WHERE tenant_id = :tenantId AND " + idColumn + " = :entityId")
                .setParameter("tenantId", TENANT_ID)
                .setParameter("entityId", entityId)
                .getSingleResult();
    }

    /**
     * Counts rows matching a WHERE condition via native SQL.
     *
     * @param tableName      the database table
     * @param whereCondition the WHERE clause (without the WHERE keyword)
     * @return the row count
     */
    private long nativeCountWhere(String tableName, String whereCondition) {
        return ((Number) entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereCondition)
                .getSingleResult())
                .longValue();
    }
}
