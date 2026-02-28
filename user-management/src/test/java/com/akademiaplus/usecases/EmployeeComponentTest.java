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
 * Component test for {@code Employee} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the Employee entity.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Employee — Component Test")
class EmployeeComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/user-management/employees";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Employee Test Academy";
    private static final String TENANT_EMAIL = "admin@employeetest.com";
    private static final String TENANT_ADDRESS = "100 Employee Blvd";

    // ── Employee test data ────────────────────────────────────────────
    private static final String EMPLOYEE_TYPE = "FULL_TIME";
    private static final String EMPLOYEE_ROLE = "ADMIN";
    private static final String EMPLOYEE_BIRTHDATE = "1990-01-15";
    private static final String EMPLOYEE_ENTRY_DATE = "2020-06-01";
    private static final String COMMON_ADDRESS = "456 Test Ave";
    private static final String COMMON_ZIP_CODE = "10000";

    private static final String EMP1_FIRST_NAME = "EmpOneFirst";
    private static final String EMP1_LAST_NAME = "EmpOneLast";
    private static final String EMP1_EMAIL = "emp1@employeetest.com";
    private static final String EMP1_PHONE = "+525599990001";
    private static final String EMP1_USERNAME = "empuser1";
    private static final String EMP1_PASSWORD = "EmpPass1!";

    private static final String EMP2_FIRST_NAME = "EmpTwoFirst";
    private static final String EMP2_LAST_NAME = "EmpTwoLast";
    private static final String EMP2_EMAIL = "emp2@employeetest.com";
    private static final String EMP2_PHONE = "+525599990002";
    private static final String EMP2_USERNAME = "empuser2";
    private static final String EMP2_PASSWORD = "EmpPass2!";

    /**
     * Table names for {@code tenant_sequences} — Employee creation
     * requires sequences for employees, person_piis, and internal_auths.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "employees", "person_piis", "internal_auths"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long employee1Id;
    private static Long employee2Id;

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
                    "employeeType": "%s",
                    "birthdate": "%s",
                    "entryDate": "%s",
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s",
                    "phoneNumber": "%s",
                    "address": "%s",
                    "zipCode": "%s",
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                }
                """.formatted(EMPLOYEE_TYPE, EMPLOYEE_BIRTHDATE, EMPLOYEE_ENTRY_DATE,
                EMP1_FIRST_NAME, EMP1_LAST_NAME, EMP1_EMAIL, EMP1_PHONE,
                COMMON_ADDRESS, COMMON_ZIP_CODE, EMP1_USERNAME, EMP1_PASSWORD, EMPLOYEE_ROLE);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").isNumber())
                .andReturn();

        // Then
        employee1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.employeeId"))
                .longValue();
        assertThat(employee1Id).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 for second employee with unique credentials")
    void shouldReturn201_whenCreatingSecondEmployee() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "employeeType": "%s",
                    "birthdate": "%s",
                    "entryDate": "%s",
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s",
                    "phoneNumber": "%s",
                    "address": "%s",
                    "zipCode": "%s",
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                }
                """.formatted(EMPLOYEE_TYPE, EMPLOYEE_BIRTHDATE, EMPLOYEE_ENTRY_DATE,
                EMP2_FIRST_NAME, EMP2_LAST_NAME, EMP2_EMAIL, EMP2_PHONE,
                COMMON_ADDRESS, COMMON_ZIP_CODE, EMP2_USERNAME, EMP2_PASSWORD, EMPLOYEE_ROLE);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").isNumber())
                .andReturn();

        // Then
        employee2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.employeeId"))
                .longValue();
        assertThat(employee2Id).isPositive();
        assertThat(employee2Id).isNotEqualTo(employee1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with employee details when found")
    void shouldReturn200_whenEmployeeFound() throws Exception {
        // Given
        assertThat(employee1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", employee1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employee1Id))
                .andExpect(jsonPath("$.firstName").value(EMP1_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(EMP1_LAST_NAME));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when employee not found")
    void shouldReturn404_whenEmployeeNotFound() throws Exception {
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
    @DisplayName("Should return 200 with list when employees exist")
    void shouldReturn200WithList_whenEmployeesExist() throws Exception {
        // Given
        assertThat(employee1Id).isNotNull();
        assertThat(employee2Id).isNotNull();
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
    @DisplayName("Should return 404 when deleting non-existent employee")
    void shouldReturn404_whenDeletingNonExistentEmployee() throws Exception {
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
    @DisplayName("Should return 204 when deleting existing employee")
    void shouldReturn204_whenDeletingExistingEmployee() throws Exception {
        // Given
        assertThat(employee2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", employee2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM employees " +
                                "WHERE tenant_id = :tenantId AND employee_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", employee2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("Employee should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when requesting soft-deleted employee by ID")
    void shouldReturn404_whenRequestingSoftDeletedEmployeeById() throws Exception {
        // Given — employee2Id was soft-deleted
        assertThat(employee2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", employee2Id)
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
