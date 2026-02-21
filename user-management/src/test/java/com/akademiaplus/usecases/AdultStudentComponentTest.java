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
 * Component test for {@code AdultStudent} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the AdultStudent entity.
 *
 * <p>AdultStudent uses customer auth (OAuth provider + token) instead of
 * internal auth (username/password) used by Employees and Collaborators.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AdultStudent — Component Test")
class AdultStudentComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/user-management/adult-students";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "AdultStudent Test Academy";
    private static final String TENANT_EMAIL = "admin@adultstudenttest.com";
    private static final String TENANT_ADDRESS = "300 Student Blvd";

    // ── AdultStudent test data ────────────────────────────────────────
    private static final String STUDENT_BIRTHDATE = "1995-07-20";
    private static final String STUDENT_PROVIDER = "INTERNAL";
    private static final String COMMON_ADDRESS = "456 Test Ave";
    private static final String COMMON_ZIP_CODE = "10000";

    private static final String STUDENT1_FIRST_NAME = "StudentOneFirst";
    private static final String STUDENT1_LAST_NAME = "StudentOneLast";
    private static final String STUDENT1_EMAIL = "student1@adultstudenttest.com";
    private static final String STUDENT1_PHONE = "+525599990021";
    private static final String STUDENT1_TOKEN = "test_token_adult_1";

    private static final String STUDENT2_FIRST_NAME = "StudentTwoFirst";
    private static final String STUDENT2_LAST_NAME = "StudentTwoLast";
    private static final String STUDENT2_EMAIL = "student2@adultstudenttest.com";
    private static final String STUDENT2_PHONE = "+525599990022";
    private static final String STUDENT2_TOKEN = "test_token_adult_2";

    /**
     * Table names for {@code tenant_sequences} — AdultStudent creation
     * requires sequences for adult_students, person_piis, and customer_auths.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "adult_students", "person_piis", "customer_auths"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long student1Id;
    private static Long student2Id;

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
    @DisplayName("Should return 201 with all fields when given complete request")
    void shouldReturn201WithAllFields_whenGivenCompleteRequest() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "birthdate": "%s",
                    "provider": "%s",
                    "token": "%s",
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s",
                    "phoneNumber": "%s",
                    "address": "%s",
                    "zipCode": "%s"
                }
                """.formatted(STUDENT_BIRTHDATE, STUDENT_PROVIDER, STUDENT1_TOKEN,
                STUDENT1_FIRST_NAME, STUDENT1_LAST_NAME, STUDENT1_EMAIL, STUDENT1_PHONE,
                COMMON_ADDRESS, COMMON_ZIP_CODE);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adultStudentId").isNumber())
                .andReturn();

        // Then
        student1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.adultStudentId"))
                .longValue();
        assertThat(student1Id).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 for second adult student with unique data")
    void shouldReturn201_whenCreatingSecondAdultStudent() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "birthdate": "%s",
                    "provider": "%s",
                    "token": "%s",
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s",
                    "phoneNumber": "%s",
                    "address": "%s",
                    "zipCode": "%s"
                }
                """.formatted(STUDENT_BIRTHDATE, STUDENT_PROVIDER, STUDENT2_TOKEN,
                STUDENT2_FIRST_NAME, STUDENT2_LAST_NAME, STUDENT2_EMAIL, STUDENT2_PHONE,
                COMMON_ADDRESS, COMMON_ZIP_CODE);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adultStudentId").isNumber())
                .andReturn();

        // Then
        student2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.adultStudentId"))
                .longValue();
        assertThat(student2Id).isPositive();
        assertThat(student2Id).isNotEqualTo(student1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with adult student details when found")
    void shouldReturn200_whenAdultStudentFound() throws Exception {
        // Given
        assertThat(student1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", student1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adultStudentId").value(student1Id))
                .andExpect(jsonPath("$.firstName").value(STUDENT1_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(STUDENT1_LAST_NAME));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when adult student not found")
    void shouldReturn404_whenAdultStudentNotFound() throws Exception {
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
    @DisplayName("Should return 200 with list when adult students exist")
    void shouldReturn200WithList_whenAdultStudentsExist() throws Exception {
        // Given
        assertThat(student1Id).isNotNull();
        assertThat(student2Id).isNotNull();
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
    @Order(6)
    @DisplayName("Should return 404 when deleting non-existent adult student")
    void shouldReturn404_whenDeletingNonExistentAdultStudent() throws Exception {
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
    @DisplayName("Should return 204 when deleting existing adult student")
    void shouldReturn204_whenDeletingExistingAdultStudent() throws Exception {
        // Given
        assertThat(student2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", student2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM adult_students " +
                                "WHERE tenant_id = :tenantId AND adult_student_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", student2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("AdultStudent should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when requesting soft-deleted adult student by ID")
    void shouldReturn404_whenRequestingSoftDeletedAdultStudentById() throws Exception {
        // Given — student2Id was soft-deleted
        assertThat(student2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", student2Id)
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
