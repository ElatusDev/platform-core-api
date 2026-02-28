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
 * Component test for {@code Course} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the Course entity.
 *
 * <p>Course creation requires existing collaborators (validated by
 * {@code CourseValidator}) and optionally schedule IDs (timeTableIds).
 * Prerequisite collaborator data is inserted directly via native SQL since
 * the course-management module does not expose collaborator creation endpoints.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Course — Component Test")
class CourseComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/course-management/courses";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Course Test Academy";
    private static final String TENANT_EMAIL = "admin@coursetest.com";
    private static final String TENANT_ADDRESS = "100 Course Blvd";

    // ── Course test data ──────────────────────────────────────────────
    private static final String COURSE1_NAME = "Mathematics 101";
    private static final String COURSE1_DESCRIPTION = "Introductory mathematics course";
    private static final int COURSE1_MAX_CAPACITY = 30;

    private static final String COURSE2_NAME = "Physics 201";
    private static final String COURSE2_DESCRIPTION = "Advanced physics course";
    private static final int COURSE2_MAX_CAPACITY = 25;

    /**
     * Table names for {@code tenant_sequences} — Course creation requires
     * sequences for courses. Prerequisite collaborators also require
     * collaborators, person_piis, and internal_auths.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "courses", "collaborators", "person_piis", "internal_auths"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long collaboratorId;
    private static Long course1Id;
    private static Long course2Id;

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

    // ── Create Tests ──────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Should return 201 when creating course with valid collaborators")
    void shouldReturn201_whenCreatingCourseWithValidCollaborators() throws Exception {
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
                """.formatted(COURSE1_NAME, COURSE1_DESCRIPTION,
                COURSE1_MAX_CAPACITY, collaboratorId);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").isNumber())
                .andReturn();

        // Then
        course1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.courseId"))
                .longValue();
        assertThat(course1Id).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 for second course with unique data")
    void shouldReturn201_whenCreatingSecondCourse() throws Exception {
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
                """.formatted(COURSE2_NAME, COURSE2_DESCRIPTION,
                COURSE2_MAX_CAPACITY, collaboratorId);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").isNumber())
                .andReturn();

        // Then
        course2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.courseId"))
                .longValue();
        assertThat(course2Id).isPositive();
        assertThat(course2Id).isNotEqualTo(course1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with course details when found")
    void shouldReturn200_whenCourseFound() throws Exception {
        // Given
        assertThat(course1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", course1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(course1Id))
                .andExpect(jsonPath("$.name").value(COURSE1_NAME))
                .andExpect(jsonPath("$.description").value(COURSE1_DESCRIPTION));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when course not found")
    void shouldReturn404_whenCourseNotFound() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        Long nonExistentId = 999999L;

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── GetAll Tests ──────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Should return 200 with list when courses exist")
    void shouldReturn200WithList_whenCoursesExist() throws Exception {
        // Given
        assertThat(course1Id).isNotNull();
        assertThat(course2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    // ── Delete Tests ──────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("Should return 404 when deleting non-existent course")
    void shouldReturn404_whenDeletingNonExistentCourse() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        Long nonExistentId = 999999L;

        // When / Then
        mockMvc.perform(delete(BASE_PATH + "/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    @Test
    @Order(7)
    @DisplayName("Should return 204 when deleting existing course")
    void shouldReturn204_whenDeletingExistingCourse() throws Exception {
        // Given
        assertThat(course2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", course2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM courses " +
                                "WHERE tenant_id = :tenantId AND course_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", course2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("Course should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when requesting soft-deleted course by ID")
    void shouldReturn404_whenRequestingSoftDeletedCourseById() throws Exception {
        // Given — course2Id was soft-deleted
        assertThat(course2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", course2Id)
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
            // Create person PII record
            entityManager.createNativeQuery(
                            "INSERT INTO person_piis "
                                    + "(tenant_id, person_pii_id, encrypted_first_name, encrypted_last_name, "
                                    + "encrypted_phone_number, encrypted_email, encrypted_address, "
                                    + "encrypted_zip_code, phone_number_hash, email_hash) "
                                    + "VALUES (:tenantId, 1, 'enc_first', 'enc_last', "
                                    + "'enc_phone', 'enc_email', 'enc_address', "
                                    + "'enc_zip', 'phone_hash_collab_course', 'email_hash_collab_course')")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            // Create internal auth record
            entityManager.createNativeQuery(
                            "INSERT INTO internal_auths "
                                    + "(tenant_id, internal_auth_id, encrypted_username, "
                                    + "encrypted_password, encrypted_role, username_hash) "
                                    + "VALUES (:tenantId, 1, 'enc_username', "
                                    + "'enc_password', 'enc_role', 'username_hash_collab_course')")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            // Create collaborator record
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
