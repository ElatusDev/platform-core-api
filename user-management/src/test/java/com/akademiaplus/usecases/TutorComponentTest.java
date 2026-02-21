/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.customer.tutor.usecases.DeleteTutorUseCase;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
 * Component test for {@code Tutor} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the Tutor entity, including the
 * business rule that prevents deletion when active minor students exist.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Tutor — Component Test")
class TutorComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/user-management/tutors";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Tutor Test Academy";
    private static final String TENANT_EMAIL = "admin@tutortest.com";
    private static final String TENANT_ADDRESS = "400 Tutor Blvd";

    // ── Tutor test data ───────────────────────────────────────────────
    private static final String TUTOR_BIRTHDATE = "1985-05-20";
    private static final String COMMON_ADDRESS = "456 Test Ave";
    private static final String COMMON_ZIP_CODE = "10000";

    private static final String TUTOR1_FIRST_NAME = "TutorOneFirst";
    private static final String TUTOR1_LAST_NAME = "TutorOneLast";
    private static final String TUTOR1_EMAIL = "tutor1@tutortest.com";
    private static final String TUTOR1_PHONE = "+525599990031";

    private static final String TUTOR2_FIRST_NAME = "TutorTwoFirst";
    private static final String TUTOR2_LAST_NAME = "TutorTwoLast";
    private static final String TUTOR2_EMAIL = "tutor2@tutortest.com";
    private static final String TUTOR2_PHONE = "+525599990032";

    // ── Minor student data (for business rule test) ───────────────────
    private static final String MINOR_PROVIDER = "INTERNAL";
    private static final String MINOR_TOKEN = "test_token_minor";
    private static final LocalDate MINOR_BIRTHDATE = LocalDate.of(2012, 7, 10);
    private static final String MINOR_FIRST_NAME = "MinorFirst";
    private static final String MINOR_LAST_NAME = "MinorLast";
    private static final String MINOR_EMAIL = "minor@tutortest.com";
    private static final String MINOR_PHONE = "+525599990033";

    /**
     * Table names for {@code tenant_sequences} — Tutor creation
     * requires sequences for tutors and person_piis. Minor student
     * creation also requires minor_students and customer_auths.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "tutors", "person_piis", "customer_auths", "minor_students"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private TutorCreationUseCase tutorCreationUseCase;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long tutorWithStudentsId;
    private static Long tutorWithoutStudentsId;

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

    // ── Create Tests ──────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Should return 201 when creating tutor with required fields")
    void shouldReturn201_whenCreatingTutorWithRequiredFields() throws Exception {
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
                TUTOR1_FIRST_NAME, TUTOR1_LAST_NAME, TUTOR1_EMAIL, TUTOR1_PHONE,
                COMMON_ADDRESS, COMMON_ZIP_CODE);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tutorId").isNumber())
                .andReturn();

        // Then
        tutorWithStudentsId = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.tutorId"))
                .longValue();
        assertThat(tutorWithStudentsId).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 for second tutor with unique data")
    void shouldReturn201_whenCreatingSecondTutor() throws Exception {
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
                TUTOR2_FIRST_NAME, TUTOR2_LAST_NAME, TUTOR2_EMAIL, TUTOR2_PHONE,
                COMMON_ADDRESS, COMMON_ZIP_CODE);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tutorId").isNumber())
                .andReturn();

        // Then
        tutorWithoutStudentsId = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.tutorId"))
                .longValue();
        assertThat(tutorWithoutStudentsId).isPositive();
        assertThat(tutorWithoutStudentsId).isNotEqualTo(tutorWithStudentsId);
    }

    @Test
    @Order(3)
    @DisplayName("Setup: Create minor student for tutor business rule test")
    void setup_createMinorStudentForTutor() {
        // Given — tutorWithStudentsId exists
        assertThat(tutorWithStudentsId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When — create minor student via use case (no REST POST endpoint)
        MinorStudentCreationRequestDTO dto = new MinorStudentCreationRequestDTO();
        dto.setBirthdate(MINOR_BIRTHDATE);
        dto.setTutorId(tutorWithStudentsId);
        dto.setFirstName(MINOR_FIRST_NAME);
        dto.setLastName(MINOR_LAST_NAME);
        dto.setEmail(MINOR_EMAIL);
        dto.setPhoneNumber(MINOR_PHONE);
        dto.setAddress(COMMON_ADDRESS);
        dto.setZipCode(COMMON_ZIP_CODE);
        dto.setProvider(MINOR_PROVIDER);
        dto.setToken(MINOR_TOKEN);
        tutorCreationUseCase.createMinorStudent(dto);

        // Then
        entityManager.clear();
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Should return 200 with tutor details when found")
    void shouldReturn200_whenTutorFound() throws Exception {
        // Given
        assertThat(tutorWithStudentsId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", tutorWithStudentsId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorId").value(tutorWithStudentsId))
                .andExpect(jsonPath("$.firstName").value(TUTOR1_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(TUTOR1_LAST_NAME));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 when tutor not found")
    void shouldReturn404_whenTutorNotFound() throws Exception {
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
    @DisplayName("Should return 200 with list when tutors exist")
    void shouldReturn200WithList_whenTutorsExist() throws Exception {
        // Given
        assertThat(tutorWithStudentsId).isNotNull();
        assertThat(tutorWithoutStudentsId).isNotNull();
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
    @DisplayName("Should return 409 when deleting tutor with active minor students")
    void shouldReturn409_whenDeletingTutorWithActiveMinorStudents() throws Exception {
        // Given — tutorWithStudentsId has at least one active minor student
        assertThat(tutorWithStudentsId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(delete(BASE_PATH + "/{id}", tutorWithStudentsId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_DELETION_BUSINESS_RULE))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString(
                        String.format(DeleteTutorUseCase.ACTIVE_MINOR_STUDENTS_REASON, 1L))));
    }

    @Test
    @Order(8)
    @DisplayName("Should return 204 when deleting tutor without active minor students")
    void shouldReturn204_whenDeletingTutorWithoutActiveMinorStudents() throws Exception {
        // Given — tutorWithoutStudentsId has no minor students
        assertThat(tutorWithoutStudentsId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", tutorWithoutStudentsId))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM tutors " +
                                "WHERE tenant_id = :tenantId AND tutor_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", tutorWithoutStudentsId)
                .getSingleResult();
        assertThat(deletedAt)
                .as("Tutor should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(9)
    @DisplayName("Should return 404 when requesting soft-deleted tutor by ID")
    void shouldReturn404_whenRequestingSoftDeletedTutorById() throws Exception {
        // Given — tutorWithoutStudentsId was soft-deleted
        assertThat(tutorWithoutStudentsId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", tutorWithoutStudentsId)
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
