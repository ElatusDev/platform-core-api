/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.customer.tutor.usecases.TutorCreationUseCase;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import jakarta.persistence.EntityManager;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentCreationRequestDTO;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for {@code MinorStudent} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the MinorStudent entity.
 *
 * <p>MinorStudent creation is performed via {@link TutorCreationUseCase#createMinorStudent}
 * since there is no direct REST POST endpoint for minor students. Read and delete
 * operations are tested via the MinorStudentController REST endpoints.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MinorStudent — Component Test")
class MinorStudentComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/user-management/minor-students";
    private static final String TUTORS_PATH = "/v1/user-management/tutors";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "MinorStudent Test Academy";
    private static final String TENANT_EMAIL = "admin@minorstudenttest.com";
    private static final String TENANT_ADDRESS = "500 Minor Blvd";

    // ── Shared test data ──────────────────────────────────────────────
    private static final String COMMON_ADDRESS = "456 Test Ave";
    private static final String COMMON_ZIP_CODE = "10000";
    private static final String MINOR_PROVIDER = "INTERNAL";
    private static final String MINOR_TOKEN = "test_token_minor";

    // ── Tutor creation data (required parent) ─────────────────────────
    private static final String TUTOR_BIRTHDATE = "1985-05-20";
    private static final String TUTOR_FIRST_NAME = "TutorParent";
    private static final String TUTOR_LAST_NAME = "ParentLast";
    private static final String TUTOR_EMAIL = "tutorparent@minorstudenttest.com";
    private static final String TUTOR_PHONE = "+525599990041";

    // ── MinorStudent test data ────────────────────────────────────────
    private static final LocalDate MINOR1_BIRTHDATE = LocalDate.of(2012, 7, 10);
    private static final String MINOR1_FIRST_NAME = "MinorOneFirst";
    private static final String MINOR1_LAST_NAME = "MinorOneLast";
    private static final String MINOR1_EMAIL = "minor1@minorstudenttest.com";
    private static final String MINOR1_PHONE = "+525599990042";

    private static final LocalDate MINOR2_BIRTHDATE = LocalDate.of(2013, 9, 25);
    private static final String MINOR2_FIRST_NAME = "MinorTwoFirst";
    private static final String MINOR2_LAST_NAME = "MinorTwoLast";
    private static final String MINOR2_EMAIL = "minor2@minorstudenttest.com";
    private static final String MINOR2_PHONE = "+525599990043";

    /**
     * Table names for {@code tenant_sequences} — MinorStudent creation
     * requires sequences for tutors, minor_students, person_piis, and customer_auths.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "tutors", "minor_students", "person_piis", "customer_auths"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private TutorCreationUseCase tutorCreationUseCase;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long tutorId;
    private static Long minor1Id;
    private static Long minor2Id;

    @BeforeEach
    void setUpTestDataOnce() throws Exception {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tenantId = createTenant(tx);
        createTenantSequences(tx);
        tenantContextHolder.setTenantId(tenantId);
        dataCreated = true;
    }

    // ── Setup: Create parent tutor ───────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Setup: Should create parent tutor for minor student tests")
    void setup_shouldCreateParentTutor() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "birthdate": "%s",
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s",
                    "phoneNumber": "%s",
                    "address": "%s",
                    "zipCode": "%s"
                }
                """.formatted(TUTOR_BIRTHDATE,
                TUTOR_FIRST_NAME, TUTOR_LAST_NAME, TUTOR_EMAIL, TUTOR_PHONE,
                COMMON_ADDRESS, COMMON_ZIP_CODE);

        // When
        MvcResult result = mockMvc.perform(post(TUTORS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tutorId").isNumber())
                .andReturn();

        // Then
        tutorId = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.tutorId"))
                .longValue();
        assertThat(tutorId).isPositive();
    }

    // ── Create Tests ──────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Should create first minor student via use case")
    void shouldCreateFirstMinorStudent_viaUseCase() {
        // Given
        assertThat(tutorId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        MinorStudentCreationRequestDTO dto = new MinorStudentCreationRequestDTO();
        dto.setBirthdate(MINOR1_BIRTHDATE);
        dto.setTutorId(tutorId);
        dto.setFirstName(MINOR1_FIRST_NAME);
        dto.setLastName(MINOR1_LAST_NAME);
        dto.setEmail(MINOR1_EMAIL);
        dto.setPhoneNumber(MINOR1_PHONE);
        dto.setAddress(COMMON_ADDRESS);
        dto.setZipCode(COMMON_ZIP_CODE);
        dto.setProvider(MINOR_PROVIDER);
        dto.setToken(MINOR_TOKEN);

        var response = tutorCreationUseCase.createMinorStudent(dto);

        // Then
        minor1Id = response.getMinorStudentId();
        assertThat(minor1Id).isPositive();
        entityManager.clear();
    }

    @Test
    @Order(3)
    @DisplayName("Should create second minor student via use case")
    void shouldCreateSecondMinorStudent_viaUseCase() {
        // Given
        assertThat(tutorId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        MinorStudentCreationRequestDTO dto = new MinorStudentCreationRequestDTO();
        dto.setBirthdate(MINOR2_BIRTHDATE);
        dto.setTutorId(tutorId);
        dto.setFirstName(MINOR2_FIRST_NAME);
        dto.setLastName(MINOR2_LAST_NAME);
        dto.setEmail(MINOR2_EMAIL);
        dto.setPhoneNumber(MINOR2_PHONE);
        dto.setAddress(COMMON_ADDRESS);
        dto.setZipCode(COMMON_ZIP_CODE);
        dto.setProvider(MINOR_PROVIDER);
        dto.setToken(MINOR_TOKEN + "_2");

        var response = tutorCreationUseCase.createMinorStudent(dto);

        // Then
        minor2Id = response.getMinorStudentId();
        assertThat(minor2Id).isPositive();
        assertThat(minor2Id).isNotEqualTo(minor1Id);
        entityManager.clear();
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Should return 200 with minor student details when found")
    void shouldReturn200_whenMinorStudentFound() throws Exception {
        // Given
        assertThat(minor1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", minor1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minorStudentId").value(minor1Id))
                .andExpect(jsonPath("$.firstName").value(MINOR1_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(MINOR1_LAST_NAME));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 when minor student not found")
    void shouldReturn404_whenMinorStudentNotFound() throws Exception {
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
    @Order(6)
    @DisplayName("Should return 200 with list when minor students exist")
    void shouldReturn200WithList_whenMinorStudentsExist() throws Exception {
        // Given
        assertThat(minor1Id).isNotNull();
        assertThat(minor2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    // ── Delete Tests ──────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("Should return 404 when deleting non-existent minor student")
    void shouldReturn404_whenDeletingNonExistentMinorStudent() throws Exception {
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
    @Order(8)
    @DisplayName("Should return 204 when deleting existing minor student")
    void shouldReturn204_whenDeletingExistingMinorStudent() throws Exception {
        // Given
        assertThat(minor2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", minor2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM minor_students " +
                                "WHERE tenant_id = :tenantId AND minor_student_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", minor2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("MinorStudent should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(9)
    @DisplayName("Should return 404 when requesting soft-deleted minor student by ID")
    void shouldReturn404_whenRequestingSoftDeletedMinorStudentById() throws Exception {
        // Given — minor2Id was soft-deleted
        assertThat(minor2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", minor2Id)
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
}
