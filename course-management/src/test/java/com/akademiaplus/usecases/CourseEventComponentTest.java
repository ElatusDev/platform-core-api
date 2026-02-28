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
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for {@code CourseEvent} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the CourseEvent entity.
 *
 * <p>CourseEvent creation requires existing Course, Schedule, and Collaborator
 * (instructor) entities. Prerequisites are created via a combination of native
 * SQL (for collaborator data) and REST endpoints (for Course and Schedule).
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("CourseEvent — Component Test")
class CourseEventComponentTest extends AbstractIntegrationTest {

    private static final String EVENTS_PATH = "/v1/course-management/course-events";
    private static final String COURSES_PATH = "/v1/course-management/courses";
    private static final String SCHEDULES_PATH = "/v1/course-management/schedules";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "CourseEvent Test Academy";
    private static final String TENANT_EMAIL = "admin@courseeventtest.com";
    private static final String TENANT_ADDRESS = "300 Event Blvd";

    // ── Course test data (prerequisite) ───────────────────────────────
    private static final String COURSE_NAME = "Event Test Course";
    private static final String COURSE_DESCRIPTION = "Course for event tests";
    private static final int COURSE_MAX_CAPACITY = 20;

    // ── Schedule test data (prerequisite) ─────────────────────────────
    private static final String SCHEDULE_DAY = "TUESDAY";
    private static final String SCHEDULE_START_TIME = "10:00:00";
    private static final String SCHEDULE_END_TIME = "11:30:00";

    // ── CourseEvent test data ─────────────────────────────────────────
    private static final String EVENT1_DATE = "2026-03-15";
    private static final String EVENT1_TITLE = "Algebra Basics";
    private static final String EVENT1_DESCRIPTION = "Introduction to algebra concepts";

    private static final String EVENT2_DATE = "2026-03-22";
    private static final String EVENT2_TITLE = "Algebra Advanced";
    private static final String EVENT2_DESCRIPTION = "Advanced algebra topics";

    /**
     * Table names for {@code tenant_sequences} — CourseEvent creation requires
     * sequences for course_events, courses, and schedules. Prerequisite
     * collaborators also require collaborators, person_piis, and internal_auths.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "course_events", "courses", "schedules",
            "collaborators", "person_piis", "internal_auths"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long collaboratorId;
    private static Long courseId;
    private static Long scheduleId;
    private static Long event1Id;
    private static Long event2Id;

    @BeforeEach
    void setUpTestDataOnce() {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tenantId = createTenant(tx);
        createTenantSequences(tx);
        collaboratorId = createPrerequisiteCollaborator(tx);
        tenantContextHolder.setTenantId(tenantId);
        dataCreated = true;
    }

    // ── Setup: Create prerequisite course and schedule ─────────────────

    @Test
    @Order(1)
    @DisplayName("Setup: Should create prerequisite course for event tests")
    void setup_shouldCreatePrerequisiteCourse() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "name": "%s",
                    "description": "%s",
                    "maxCapacity": %d,
                    "timeTableIds": [],
                    "availableCollaboratorIds": [%d]
                }
                """.formatted(COURSE_NAME, COURSE_DESCRIPTION,
                COURSE_MAX_CAPACITY, collaboratorId);

        // When
        MvcResult result = mockMvc.perform(post(COURSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").isNumber())
                .andReturn();

        // Then
        courseId = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.courseId"))
                .longValue();
        assertThat(courseId).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Setup: Should create prerequisite schedule for event tests")
    void setup_shouldCreatePrerequisiteSchedule() throws Exception {
        // Given
        assertThat(courseId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "scheduleDay": "%s",
                    "startTime": "%s",
                    "endTime": "%s",
                    "courseId": %d
                }
                """.formatted(SCHEDULE_DAY, SCHEDULE_START_TIME,
                SCHEDULE_END_TIME, courseId);

        // When
        MvcResult result = mockMvc.perform(post(SCHEDULES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleId").isNumber())
                .andReturn();

        // Then
        scheduleId = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.scheduleId"))
                .longValue();
        assertThat(scheduleId).isPositive();
    }

    // ── Create Tests ──────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 201 when creating course event with valid references")
    void shouldReturn201_whenCreatingCourseEventWithValidReferences() throws Exception {
        // Given
        assertThat(courseId).isNotNull();
        assertThat(scheduleId).isNotNull();
        assertThat(collaboratorId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "date": "%s",
                    "title": "%s",
                    "description": "%s",
                    "scheduleId": %d,
                    "courseId": %d,
                    "instructorId": %d,
                    "adultAttendeeIds": [],
                    "minorAttendeeIds": []
                }
                """.formatted(EVENT1_DATE, EVENT1_TITLE, EVENT1_DESCRIPTION,
                scheduleId, courseId, collaboratorId);

        // When
        MvcResult result = mockMvc.perform(post(EVENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseEventId").isNumber())
                .andReturn();

        // Then
        event1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.courseEventId"))
                .longValue();
        assertThat(event1Id).isPositive();
    }

    @Test
    @Order(4)
    @DisplayName("Should return 201 for second course event with unique data")
    void shouldReturn201_whenCreatingSecondCourseEvent() throws Exception {
        // Given
        assertThat(courseId).isNotNull();
        assertThat(scheduleId).isNotNull();
        assertThat(collaboratorId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "date": "%s",
                    "title": "%s",
                    "description": "%s",
                    "scheduleId": %d,
                    "courseId": %d,
                    "instructorId": %d,
                    "adultAttendeeIds": [],
                    "minorAttendeeIds": []
                }
                """.formatted(EVENT2_DATE, EVENT2_TITLE, EVENT2_DESCRIPTION,
                scheduleId, courseId, collaboratorId);

        // When
        MvcResult result = mockMvc.perform(post(EVENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseEventId").isNumber())
                .andReturn();

        // Then
        event2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.courseEventId"))
                .longValue();
        assertThat(event2Id).isPositive();
        assertThat(event2Id).isNotEqualTo(event1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Should return 200 with course event details when found")
    void shouldReturn200_whenCourseEventFound() throws Exception {
        // Given
        assertThat(event1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(EVENTS_PATH + "/{id}", event1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseEventId").value(event1Id))
                .andExpect(jsonPath("$.title").value(EVENT1_TITLE))
                .andExpect(jsonPath("$.description").value(EVENT1_DESCRIPTION));
    }

    @Test
    @Order(6)
    @DisplayName("Should return 404 when course event not found")
    void shouldReturn404_whenCourseEventNotFound() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        Long nonExistentId = 999999L;

        // When / Then
        mockMvc.perform(get(EVENTS_PATH + "/{id}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── GetAll Tests ──────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("Should return 200 with list when course events exist")
    void shouldReturn200WithList_whenCourseEventsExist() throws Exception {
        // Given
        assertThat(event1Id).isNotNull();
        assertThat(event2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(EVENTS_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    // ── Delete Tests ──────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("Should return 404 when deleting non-existent course event")
    void shouldReturn404_whenDeletingNonExistentCourseEvent() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        Long nonExistentId = 999999L;

        // When / Then
        mockMvc.perform(delete(EVENTS_PATH + "/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    @Test
    @Order(9)
    @DisplayName("Should return 204 when deleting existing course event")
    void shouldReturn204_whenDeletingExistingCourseEvent() throws Exception {
        // Given
        assertThat(event2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(EVENTS_PATH + "/{id}", event2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM course_events " +
                                "WHERE tenant_id = :tenantId AND course_event_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", event2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("CourseEvent should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(10)
    @DisplayName("Should return 404 when requesting soft-deleted course event by ID")
    void shouldReturn404_whenRequestingSoftDeletedCourseEventById() throws Exception {
        // Given — event2Id was soft-deleted
        assertThat(event2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(EVENTS_PATH + "/{id}", event2Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── Data Creation Helpers ────────────────────────────────────────

    private Long createTenant(TransactionTemplate tx) {
        return tx.execute(status -> {
            TenantDataModel tenant = new TenantDataModel();
            tenant.setOrganizationName(TENANT_ORG_NAME);
            tenant.setEmail(TENANT_EMAIL);
            tenant.setAddress(TENANT_ADDRESS);
            entityManager.persist(tenant);
            entityManager.flush();
            return tenant.getTenantId();
        });
    }

    private void createTenantSequences(TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            for (String tableName : ENTITY_TABLE_NAMES) {
                entityManager.createNativeQuery(
                                "INSERT INTO tenant_sequences "
                                        + "(tenant_id, entity_name, next_value, version) "
                                        + "VALUES (:tenantId, :entityName, 1, 0)")
                        .setParameter("tenantId", tenantId)
                        .setParameter("entityName", tableName)
                        .executeUpdate();
            }
        });
    }

    /**
     * Creates a prerequisite collaborator directly via native SQL.
     * CourseEvent creation requires an instructor (collaborator) reference.
     * The collaborator also satisfies Course's {@code availableCollaboratorIds}.
     *
     * @return the collaborator ID of the created collaborator
     */
    private Long createPrerequisiteCollaborator(TransactionTemplate tx) {
        return tx.execute(status -> {
            entityManager.createNativeQuery(
                            "INSERT INTO person_piis "
                                    + "(tenant_id, person_pii_id, encrypted_first_name, encrypted_last_name, "
                                    + "encrypted_phone_number, encrypted_email, encrypted_address, "
                                    + "encrypted_zip_code, phone_number_hash, email_hash) "
                                    + "VALUES (:tenantId, 1, 'enc_first', 'enc_last', "
                                    + "'enc_phone', 'enc_email', 'enc_address', "
                                    + "'enc_zip', 'phone_hash_collab_evt', 'email_hash_collab_evt')")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            entityManager.createNativeQuery(
                            "INSERT INTO internal_auths "
                                    + "(tenant_id, internal_auth_id, encrypted_username, "
                                    + "encrypted_password, encrypted_role, username_hash) "
                                    + "VALUES (:tenantId, 1, 'enc_username', "
                                    + "'enc_password', 'enc_role', 'username_hash_collab_evt')")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            entityManager.createNativeQuery(
                            "INSERT INTO collaborators "
                                    + "(tenant_id, collaborator_id, internal_auth_id, skills, "
                                    + "birthdate, entry_date, person_pii_id) "
                                    + "VALUES (:tenantId, 1, 1, 'Mathematics', "
                                    + "'1985-05-20', '2020-01-15', 1)")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            return 1L;
        });
    }
}
