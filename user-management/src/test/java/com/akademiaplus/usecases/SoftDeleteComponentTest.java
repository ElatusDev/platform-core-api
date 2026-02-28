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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tier 3 component test for soft-delete behavior in the user-management module.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and exercises delete endpoints end-to-end. Test data is created via creation
 * endpoints and use cases in {@code @BeforeAll}, then delete operations are
 * verified in ordered test methods.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Step 7.1: {@code @SQLDelete} targets only the single entity row</li>
 *   <li>Step 7.2: {@code @SQLRestriction} excludes soft-deleted entities from queries</li>
 *   <li>Step 7.4a: Tutor pre-delete business rule returns HTTP 409 when active
 *       MinorStudents exist</li>
 *   <li>Step 7.4b: Tutor deletion returns HTTP 204 when no active MinorStudents</li>
 * </ul>
 *
 * <p>Tests are ordered so that the soft-deleted employee from Step 7.1 remains
 * visible to Step 7.2's exclusion verification. Test data is created once
 * in a flag-guarded {@code @BeforeEach} method to ensure the Spring context
 * and Testcontainers are fully initialized before data setup runs.
 *
 * @author ElatusDev
 * @since 1.0
 * @see AbstractIntegrationTest
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Soft-Delete — User Management Component Test")
class SoftDeleteComponentTest extends AbstractIntegrationTest {

    private static final String EMPLOYEES_PATH = "/v1/user-management/employees";
    private static final String TUTORS_PATH = "/v1/user-management/tutors";

    // ── Tenant creation data ─────────────────────────────────────────────

    private static final String TENANT_ORG_NAME = "Test Tenant";
    private static final String TENANT_EMAIL = "tenant@test.com";
    private static final String TENANT_ADDRESS = "123 Test St";

    // ── Shared test data ─────────────────────────────────────────────────

    private static final String COMMON_ADDRESS = "456 Test Ave";
    private static final String COMMON_ZIP_CODE = "10000";

    // ── Employee creation data ───────────────────────────────────────────

    private static final String EMPLOYEE_TYPE = "FULL_TIME";
    private static final String EMPLOYEE_ROLE = "ADMIN";
    private static final String EMPLOYEE_BIRTHDATE = "1990-01-01";
    private static final String EMPLOYEE_ENTRY_DATE = "2020-06-15";

    private static final String EMPLOYEE1_FIRST_NAME = "EmpOneFirst";
    private static final String EMPLOYEE1_LAST_NAME = "EmpOneLast";
    private static final String EMPLOYEE1_EMAIL = "emp1@test.com";
    private static final String EMPLOYEE1_PHONE = "+525512345601";
    private static final String EMPLOYEE1_USERNAME = "testuser1";
    private static final String EMPLOYEE1_PASSWORD = "TestPass1!";

    private static final String EMPLOYEE2_FIRST_NAME = "EmpTwoFirst";
    private static final String EMPLOYEE2_LAST_NAME = "EmpTwoLast";
    private static final String EMPLOYEE2_EMAIL = "emp2@test.com";
    private static final String EMPLOYEE2_PHONE = "+525512345602";
    private static final String EMPLOYEE2_USERNAME = "testuser2";
    private static final String EMPLOYEE2_PASSWORD = "TestPass2!";

    // ── Tutor creation data ──────────────────────────────────────────────

    private static final String TUTOR_BIRTHDATE = "1985-05-20";

    private static final String TUTOR1_FIRST_NAME = "TutorOneFirst";
    private static final String TUTOR1_LAST_NAME = "TutorOneLast";
    private static final String TUTOR1_EMAIL = "tutor1@test.com";
    private static final String TUTOR1_PHONE = "+525512345603";

    private static final String TUTOR2_FIRST_NAME = "TutorTwoFirst";
    private static final String TUTOR2_LAST_NAME = "TutorTwoLast";
    private static final String TUTOR2_EMAIL = "tutor2@test.com";
    private static final String TUTOR2_PHONE = "+525512345604";

    // ── Minor student creation data ──────────────────────────────────────

    private static final String MINOR_STUDENT_PROVIDER = "INTERNAL";
    private static final String MINOR_STUDENT_TOKEN = "test_token";
    private static final LocalDate MINOR1_BIRTHDATE = LocalDate.of(2012, 7, 10);
    private static final String MINOR1_FIRST_NAME = "MinorOneFirst";
    private static final String MINOR1_LAST_NAME = "MinorOneLast";
    private static final String MINOR1_EMAIL = "ms1@test.com";
    private static final String MINOR1_PHONE = "+525512345605";

    private static final LocalDate MINOR2_BIRTHDATE = LocalDate.of(2013, 9, 25);
    private static final String MINOR2_FIRST_NAME = "MinorTwoFirst";
    private static final String MINOR2_LAST_NAME = "MinorTwoLast";
    private static final String MINOR2_EMAIL = "ms2@test.com";
    private static final String MINOR2_PHONE = "+525512345606";

    /**
     * Table names matching {@code @Table(name)} annotations on entity classes.
     * Used to seed the {@code tenant_sequences} table so that
     * {@code EntityIdAssigner} can generate IDs for each entity type.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "employees", "tutors", "minor_students",
            "person_piis", "internal_auths", "customer_auths"
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private TutorCreationUseCase tutorCreationUseCase;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** Guards one-time data creation across test instances (PER_METHOD lifecycle). */
    private static boolean dataCreated;

    /** Tenant ID assigned by AUTO_INCREMENT during test setup. */
    private static Long tenantId;

    /** Employee IDs assigned by {@code EntityIdAssigner} during test setup. */
    private static Long employeeId1;
    private static Long employeeId2;

    /** Tutor IDs assigned by {@code EntityIdAssigner} during test setup. */
    private static Long tutorIdWithMinorStudents;
    private static Long tutorIdWithoutMinorStudents;

    /**
     * Creates all test data once via creation endpoints and use cases.
     *
     * <p>Uses a static flag to ensure data is created only on the first
     * invocation. Runs as {@code @BeforeEach} instead of {@code @BeforeAll}
     * so that the Spring context and Testcontainers are fully initialized.
     *
     * <p>Uses {@code TransactionTemplate} for direct EntityManager operations
     * (tenant and sequences), MockMvc POST for employees and tutors, and
     * direct use case invocation for minor students (no REST endpoint wired).
     */
    @BeforeEach
    void setUpTestDataOnce() throws Exception {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tenantId = createTenant(tx);
        createTenantSequences(tx);
        tenantContextHolder.setTenantId(tenantId);

        employeeId1 = createEmployee(EMPLOYEE1_FIRST_NAME, EMPLOYEE1_LAST_NAME,
                EMPLOYEE1_EMAIL, EMPLOYEE1_PHONE, EMPLOYEE1_USERNAME, EMPLOYEE1_PASSWORD);
        employeeId2 = createEmployee(EMPLOYEE2_FIRST_NAME, EMPLOYEE2_LAST_NAME,
                EMPLOYEE2_EMAIL, EMPLOYEE2_PHONE, EMPLOYEE2_USERNAME, EMPLOYEE2_PASSWORD);

        tutorIdWithMinorStudents = createTutor(TUTOR1_FIRST_NAME, TUTOR1_LAST_NAME,
                TUTOR1_EMAIL, TUTOR1_PHONE);
        tutorIdWithoutMinorStudents = createTutor(TUTOR2_FIRST_NAME, TUTOR2_LAST_NAME,
                TUTOR2_EMAIL, TUTOR2_PHONE);

        createMinorStudent(MINOR1_FIRST_NAME, MINOR1_LAST_NAME,
                MINOR1_EMAIL, MINOR1_PHONE, MINOR1_BIRTHDATE, tutorIdWithMinorStudents);
        createMinorStudent(MINOR2_FIRST_NAME, MINOR2_LAST_NAME,
                MINOR2_EMAIL, MINOR2_PHONE, MINOR2_BIRTHDATE, tutorIdWithMinorStudents);

        entityManager.clear();
        dataCreated = true;
    }

    // ── Step 7.1: @SQLDelete Single-Row Verification ─────────────────────

    @Test
    @Order(1)
    @DisplayName("Step 7.1 — Should soft-delete only target employee, not all employees for tenant")
    void shouldSoftDeleteOnlyTargetEmployee_whenDeleteEndpointCalled() throws Exception {
        // Given — two employees exist, both with deleted_at IS NULL
        assertThat(nativeDeletedAt("employees", "employee_id", employeeId1))
                .as("employee1 should not be soft-deleted before test")
                .isNull();
        assertThat(nativeDeletedAt("employees", "employee_id", employeeId2))
                .as("employee2 should not be soft-deleted before test")
                .isNull();

        tenantContextHolder.setTenantId(tenantId);

        // When — delete employee1
        mockMvc.perform(delete(EMPLOYEES_PATH + "/{employeeId}", employeeId1))
                .andDo(print())
                .andExpect(status().isNoContent());

        entityManager.clear();

        // Then — employee1 has deleted_at set, employee2 has deleted_at NULL
        assertThat(nativeDeletedAt("employees", "employee_id", employeeId1))
                .as("employee1 should have deleted_at set after deletion")
                .isNotNull();
        assertThat(nativeDeletedAt("employees", "employee_id", employeeId2))
                .as("employee2 should still have deleted_at NULL (untouched)")
                .isNull();
    }

    // ── Step 7.2: @SQLRestriction Exclusion ──────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Step 7.2 — Should return 404 when requesting soft-deleted employee by ID")
    void shouldReturn404_whenRequestingSoftDeletedEmployeeById() throws Exception {
        // Given — employee1 was soft-deleted in Step 7.1
        tenantContextHolder.setTenantId(tenantId);

        // When & Then — GET by ID should return 404
        mockMvc.perform(get(EMPLOYEES_PATH + "/{employeeId}", employeeId1)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── Step 7.4a: Tutor Business Rule — 409 ────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Step 7.4a — Should return 409 with DELETION_BUSINESS_RULE when tutor has active minor students")
    void shouldReturn409WithBusinessRule_whenTutorHasActiveMinorStudents() throws Exception {
        // Given — tutor1 has 2 active minor students
        long activeMinorStudents = nativeCountWhere(
                "minor_students", "tutor_id = " + tutorIdWithMinorStudents
                        + " AND tenant_id = " + tenantId + " AND deleted_at IS NULL");
        assertThat(activeMinorStudents)
                .as("tutor should have at least one active minor student")
                .isGreaterThan(0);

        tenantContextHolder.setTenantId(tenantId);

        // When & Then — DELETE tutor -> 409 with business rule code
        mockMvc.perform(delete(TUTORS_PATH + "/{tutorId}", tutorIdWithMinorStudents))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_DELETION_BUSINESS_RULE))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString(
                        String.format(DeleteTutorUseCase.ACTIVE_MINOR_STUDENTS_REASON,
                                activeMinorStudents))));
    }

    // ── Step 7.4b: Tutor Deletion Success — 204 ─────────────────────────

    @Test
    @Order(4)
    @DisplayName("Step 7.4b — Should return 204 when tutor has no active minor students")
    void shouldReturn204_whenTutorHasNoActiveMinorStudents() throws Exception {
        // Given — tutor2 has zero minor students
        long activeMinorStudents = nativeCountWhere(
                "minor_students", "tutor_id = " + tutorIdWithoutMinorStudents
                        + " AND tenant_id = " + tenantId + " AND deleted_at IS NULL");
        assertThat(activeMinorStudents)
                .as("tutor should have zero active minor students")
                .isZero();

        tenantContextHolder.setTenantId(tenantId);

        // When & Then — DELETE tutor -> 204 (no active students)
        mockMvc.perform(delete(TUTORS_PATH + "/{tutorId}", tutorIdWithoutMinorStudents))
                .andExpect(status().isNoContent());

        entityManager.clear();

        // Then — tutor is soft-deleted
        assertThat(nativeDeletedAt("tutors", "tutor_id", tutorIdWithoutMinorStudents))
                .as("tutor should have deleted_at set after successful deletion")
                .isNotNull();
    }

    // ── Data Creation Helpers ────────────────────────────────────────────

    /**
     * Persists a {@link TenantDataModel} via EntityManager.
     * Uses AUTO_INCREMENT (not {@code EntityIdAssigner}).
     *
     * @param tx the transaction template for EntityManager writes
     * @return the generated tenant ID
     */
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

    /**
     * Inserts {@code tenant_sequences} rows for all entity types so that
     * {@code EntityIdAssigner} can generate sequential IDs during creation.
     *
     * @param tx the transaction template for EntityManager writes
     */
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
     * Creates an employee via {@code POST /v1/user-management/employees}.
     *
     * @return the generated employee ID from the response
     */
    private Long createEmployee(String firstName, String lastName, String email,
                                String phone, String username, String password)
            throws Exception {
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
                firstName, lastName, email, phone, COMMON_ADDRESS, COMMON_ZIP_CODE,
                username, password, EMPLOYEE_ROLE);

        MvcResult result = mockMvc.perform(post(EMPLOYEES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.employeeId"))
                .longValue();
    }

    /**
     * Creates a tutor via {@code POST /v1/user-management/tutors}.
     *
     * @return the generated tutor ID from the response
     */
    private Long createTutor(String firstName, String lastName,
                             String email, String phone) throws Exception {
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
                """.formatted(TUTOR_BIRTHDATE, firstName, lastName,
                email, phone, COMMON_ADDRESS, COMMON_ZIP_CODE);

        MvcResult result = mockMvc.perform(post(TUTORS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.tutorId"))
                .longValue();
    }

    /**
     * Creates a minor student via direct use case invocation.
     * No REST endpoint is wired for minor student creation, so this
     * bypasses the controller layer and calls the use case directly.
     *
     * @param firstName the minor student's first name
     * @param lastName  the minor student's last name
     * @param email     the minor student's email
     * @param phone     the minor student's phone number
     * @param birthdate the minor student's birthdate
     * @param tutorId   the ID of the tutor to link the minor student to
     */
    private void createMinorStudent(String firstName, String lastName,
                                    String email, String phone,
                                    LocalDate birthdate, Long tutorId) {
        MinorStudentCreationRequestDTO dto = new MinorStudentCreationRequestDTO();
        dto.setBirthdate(birthdate);
        dto.setTutorId(tutorId);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setEmail(email);
        dto.setPhoneNumber(phone);
        dto.setAddress(COMMON_ADDRESS);
        dto.setZipCode(COMMON_ZIP_CODE);
        dto.setProvider(MINOR_STUDENT_PROVIDER);
        dto.setToken(MINOR_STUDENT_TOKEN);
        tutorCreationUseCase.createMinorStudent(dto);
    }

    // ── Native SQL Helpers ───────────────────────────────────────────────

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
                .setParameter("tenantId", tenantId)
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
