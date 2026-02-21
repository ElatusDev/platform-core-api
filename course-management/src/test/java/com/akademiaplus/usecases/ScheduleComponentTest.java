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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for {@code Schedule} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the Schedule entity.
 *
 * <p>Schedule creation requires an existing Course (resolved via FK lookup).
 * The prerequisite Course is created via the REST endpoint, which in turn
 * requires a prerequisite collaborator inserted via native SQL.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Schedule — Component Test")
class ScheduleComponentTest extends AbstractIntegrationTest {

    private static final String SCHEDULES_PATH = "/v1/course-management/schedules";
    private static final String COURSES_PATH = "/v1/course-management/courses";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Schedule Test Academy";
    private static final String TENANT_EMAIL = "admin@scheduletest.com";
    private static final String TENANT_ADDRESS = "200 Schedule Blvd";

    // ── Course test data (prerequisite) ───────────────────────────────
    private static final String COURSE_NAME = "Schedule Test Course";
    private static final String COURSE_DESCRIPTION = "Course for schedule tests";
    private static final int COURSE_MAX_CAPACITY = 20;

    // ── Schedule test data ────────────────────────────────────────────
    private static final String SCHEDULE1_DAY = "MONDAY";
    private static final String SCHEDULE1_START_TIME = "09:00:00";
    private static final String SCHEDULE1_END_TIME = "10:30:00";

    private static final String SCHEDULE2_DAY = "WEDNESDAY";
    private static final String SCHEDULE2_START_TIME = "14:00:00";
    private static final String SCHEDULE2_END_TIME = "15:30:00";

    /**
     * Table names for {@code tenant_sequences} — Schedule creation requires
     * sequences for schedules and courses. Prerequisite collaborators also
     * require collaborators, person_piis, and internal_auths.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "schedules", "courses", "collaborators", "person_piis", "internal_auths"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long collaboratorId;
    private static Long courseId;
    private static Long schedule1Id;
    private static Long schedule2Id;

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

    // ── Setup: Create prerequisite course ──────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Setup: Should create prerequisite course for schedule tests")
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

    // ── Create Tests ──────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Should return 201 when creating schedule with valid course")
    void shouldReturn201_whenCreatingScheduleWithValidCourse() throws Exception {
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
                """.formatted(SCHEDULE1_DAY, SCHEDULE1_START_TIME,
                SCHEDULE1_END_TIME, courseId);

        // When
        MvcResult result = mockMvc.perform(post(SCHEDULES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleId").isNumber())
                .andReturn();

        // Then
        schedule1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.scheduleId"))
                .longValue();
        assertThat(schedule1Id).isPositive();
    }

    @Test
    @Order(3)
    @DisplayName("Should return 201 for second schedule with unique data")
    void shouldReturn201_whenCreatingSecondSchedule() throws Exception {
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
                """.formatted(SCHEDULE2_DAY, SCHEDULE2_START_TIME,
                SCHEDULE2_END_TIME, courseId);

        // When
        MvcResult result = mockMvc.perform(post(SCHEDULES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleId").isNumber())
                .andReturn();

        // Then
        schedule2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.scheduleId"))
                .longValue();
        assertThat(schedule2Id).isPositive();
        assertThat(schedule2Id).isNotEqualTo(schedule1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Should return 200 with schedule details when found")
    void shouldReturn200_whenScheduleFound() throws Exception {
        // Given
        assertThat(schedule1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(SCHEDULES_PATH + "/{id}", schedule1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(schedule1Id))
                .andExpect(jsonPath("$.scheduleDay").value(SCHEDULE1_DAY));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 when schedule not found")
    void shouldReturn404_whenScheduleNotFound() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        Long nonExistentId = 999999L;

        // When / Then
        mockMvc.perform(get(SCHEDULES_PATH + "/{id}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── GetAll Tests ──────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("Should return 200 with list when schedules exist")
    void shouldReturn200WithList_whenSchedulesExist() throws Exception {
        // Given
        assertThat(schedule1Id).isNotNull();
        assertThat(schedule2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(SCHEDULES_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    // ── Delete Tests ──────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("Should return 404 when deleting non-existent schedule")
    void shouldReturn404_whenDeletingNonExistentSchedule() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        Long nonExistentId = 999999L;

        // When / Then
        mockMvc.perform(delete(SCHEDULES_PATH + "/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    @Test
    @Order(8)
    @DisplayName("Should return 204 when deleting existing schedule")
    void shouldReturn204_whenDeletingExistingSchedule() throws Exception {
        // Given
        assertThat(schedule2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(SCHEDULES_PATH + "/{id}", schedule2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM schedules " +
                                "WHERE tenant_id = :tenantId AND schedule_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", schedule2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("Schedule should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(9)
    @DisplayName("Should return 404 when requesting soft-deleted schedule by ID")
    void shouldReturn404_whenRequestingSoftDeletedScheduleById() throws Exception {
        // Given — schedule2Id was soft-deleted
        assertThat(schedule2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(SCHEDULES_PATH + "/{id}", schedule2Id)
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
     * Course creation requires at least one existing collaborator for the
     * {@code availableCollaboratorIds} field.
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
                                    + "'enc_zip', 'phone_hash_collab_sched', 'email_hash_collab_sched')")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            entityManager.createNativeQuery(
                            "INSERT INTO internal_auths "
                                    + "(tenant_id, internal_auth_id, encrypted_username, "
                                    + "encrypted_password, encrypted_role, username_hash) "
                                    + "VALUES (:tenantId, 1, 'enc_username', "
                                    + "'enc_password', 'enc_role', 'username_hash_collab_sched')")
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
