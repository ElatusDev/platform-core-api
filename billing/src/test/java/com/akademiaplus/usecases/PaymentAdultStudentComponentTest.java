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
 * Component test for {@code PaymentAdultStudent} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the PaymentAdultStudent entity.
 *
 * <p>PaymentAdultStudent creation requires an existing MembershipAdultStudent.
 * The prerequisite chain (Membership, Course, AdultStudent, MembershipAdultStudent)
 * is created via a combination of native SQL and REST endpoints.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PaymentAdultStudent — Component Test")
class PaymentAdultStudentComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/billing/paymentAdultStudents";
    private static final String MEMBERSHIPS_PATH = "/v1/billing/memberships";
    private static final String MAS_PATH = "/v1/billing/membershipAdultStudents";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "PaymentAdultStudent Test Academy";
    private static final String TENANT_EMAIL = "admin@pastest.com";
    private static final String TENANT_ADDRESS = "500 PAS Blvd";

    // ── Prerequisite data ─────────────────────────────────────────────
    private static final String MEMBERSHIP_TYPE = "MONTHLY";
    private static final double MEMBERSHIP_FEE = 500.00;
    private static final String MEMBERSHIP_DESCRIPTION = "Membership for PAS test";
    private static final String MAS_START_DATE = "2026-01-01";
    private static final String MAS_DUE_DATE = "2026-01-31";

    // ── PaymentAdultStudent test data ─────────────────────────────────
    private static final String PAYMENT1_DATE = "2026-01-15";
    private static final double PAYMENT1_AMOUNT = 500.00;
    private static final String PAYMENT1_METHOD = "CREDIT_CARD";

    private static final String PAYMENT2_DATE = "2026-01-20";
    private static final double PAYMENT2_AMOUNT = 250.00;
    private static final String PAYMENT2_METHOD = "CASH";

    /**
     * Table names for {@code tenant_sequences}.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "payment_adult_students", "membership_adult_students", "memberships",
            "courses", "adult_students", "person_piis", "customer_auths",
            "collaborators", "internal_auths"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long membershipId;
    private static Long membershipAdultStudentId;
    private static final Long COURSE_ID = 1L;
    private static final Long ADULT_STUDENT_ID = 1L;
    private static Long payment1Id;
    private static Long payment2Id;

    @BeforeEach
    void setUpTestDataOnce() {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tenantId = createTenant(tx);
        createTenantSequences(tx);
        createPrerequisiteCourseData(tx);
        createPrerequisiteAdultStudentData(tx);
        tenantContextHolder.setTenantId(tenantId);
        dataCreated = true;
    }

    // ── Setup: Create prerequisite membership chain ────────────────────

    @Test
    @Order(1)
    @DisplayName("Setup: Should create prerequisite membership")
    void setup_shouldCreatePrerequisiteMembership() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "membershipType": "%s",
                    "fee": %s,
                    "description": "%s"
                }
                """.formatted(MEMBERSHIP_TYPE, MEMBERSHIP_FEE, MEMBERSHIP_DESCRIPTION);

        // When
        MvcResult result = mockMvc.perform(post(MEMBERSHIPS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        // Then
        membershipId = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.membershipId"))
                .longValue();
        assertThat(membershipId).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Setup: Should create prerequisite membership-adult-student")
    void setup_shouldCreatePrerequisiteMembershipAdultStudent() throws Exception {
        // Given
        assertThat(membershipId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "startDate": "%s",
                    "dueDate": "%s",
                    "membershipId": %d,
                    "courseId": %d,
                    "adultStudentId": %d
                }
                """.formatted(MAS_START_DATE, MAS_DUE_DATE,
                membershipId, COURSE_ID, ADULT_STUDENT_ID);

        // When
        MvcResult result = mockMvc.perform(post(MAS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        // Then
        membershipAdultStudentId = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.membershipAdultStudentId"))
                .longValue();
        assertThat(membershipAdultStudentId).isPositive();
    }

    // ── Create Tests ──────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 201 when creating payment with valid membership-adult-student")
    void shouldReturn201_whenCreatingWithValidReference() throws Exception {
        // Given
        assertThat(membershipAdultStudentId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "paymentDate": "%s",
                    "amount": %s,
                    "paymentMethod": "%s",
                    "membershipAdultStudentId": %d
                }
                """.formatted(PAYMENT1_DATE, PAYMENT1_AMOUNT,
                PAYMENT1_METHOD, membershipAdultStudentId);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentAdultStudentId").isNumber())
                .andReturn();

        // Then
        payment1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.paymentAdultStudentId"))
                .longValue();
        assertThat(payment1Id).isPositive();
    }

    @Test
    @Order(4)
    @DisplayName("Should return 201 for second payment with unique data")
    void shouldReturn201_whenCreatingSecondPayment() throws Exception {
        // Given
        assertThat(membershipAdultStudentId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "paymentDate": "%s",
                    "amount": %s,
                    "paymentMethod": "%s",
                    "membershipAdultStudentId": %d
                }
                """.formatted(PAYMENT2_DATE, PAYMENT2_AMOUNT,
                PAYMENT2_METHOD, membershipAdultStudentId);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentAdultStudentId").isNumber())
                .andReturn();

        // Then
        payment2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.paymentAdultStudentId"))
                .longValue();
        assertThat(payment2Id).isPositive();
        assertThat(payment2Id).isNotEqualTo(payment1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Should return 200 with payment details when found")
    void shouldReturn200_whenPaymentAdultStudentFound() throws Exception {
        // Given
        assertThat(payment1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", payment1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentAdultStudentId").value(payment1Id));
    }

    @Test
    @Order(6)
    @DisplayName("Should return 404 when payment-adult-student not found")
    void shouldReturn404_whenPaymentAdultStudentNotFound() throws Exception {
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
    @Order(7)
    @DisplayName("Should return 200 with list when payment-adult-students exist")
    void shouldReturn200WithList_whenPaymentAdultStudentsExist() throws Exception {
        // Given
        assertThat(payment1Id).isNotNull();
        assertThat(payment2Id).isNotNull();
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
    @Order(8)
    @DisplayName("Should return 404 when deleting non-existent payment-adult-student")
    void shouldReturn404_whenDeletingNonExistentPaymentAdultStudent() throws Exception {
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
    @Order(9)
    @DisplayName("Should return 204 when deleting existing payment-adult-student")
    void shouldReturn204_whenDeletingExistingPaymentAdultStudent() throws Exception {
        // Given
        assertThat(payment2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", payment2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM payment_adult_students " +
                                "WHERE tenant_id = :tenantId AND payment_adult_student_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", payment2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("PaymentAdultStudent should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(10)
    @DisplayName("Should return 404 when requesting soft-deleted payment-adult-student by ID")
    void shouldReturn404_whenRequestingSoftDeletedPaymentAdultStudentById() throws Exception {
        // Given — payment2Id was soft-deleted
        assertThat(payment2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", payment2Id)
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

    private void createPrerequisiteCourseData(TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                            "INSERT INTO person_piis "
                                    + "(tenant_id, person_pii_id, encrypted_first_name, encrypted_last_name, "
                                    + "encrypted_phone_number, encrypted_email, encrypted_address, "
                                    + "encrypted_zip_code, phone_number_hash, email_hash) "
                                    + "VALUES (:tenantId, 1, 'enc_first', 'enc_last', "
                                    + "'enc_phone', 'enc_email', 'enc_address', "
                                    + "'enc_zip', 'phone_hash_pas', 'email_hash_pas')")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            entityManager.createNativeQuery(
                            "INSERT INTO internal_auths "
                                    + "(tenant_id, internal_auth_id, encrypted_username, "
                                    + "encrypted_password, encrypted_role, username_hash) "
                                    + "VALUES (:tenantId, 1, 'enc_username', "
                                    + "'enc_password', 'enc_role', 'username_hash_pas')")
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

            entityManager.createNativeQuery(
                            "INSERT INTO courses "
                                    + "(tenant_id, course_id, course_name, course_description, max_capacity) "
                                    + "VALUES (:tenantId, :courseId, 'PAS Test Course', 'Course for PAS test', 30)")
                    .setParameter("tenantId", tenantId)
                    .setParameter("courseId", COURSE_ID)
                    .executeUpdate();
        });
    }

    private void createPrerequisiteAdultStudentData(TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                            "INSERT INTO person_piis "
                                    + "(tenant_id, person_pii_id, encrypted_first_name, encrypted_last_name, "
                                    + "encrypted_phone_number, encrypted_email, encrypted_address, "
                                    + "encrypted_zip_code, phone_number_hash, email_hash) "
                                    + "VALUES (:tenantId, 2, 'enc_student_first', 'enc_student_last', "
                                    + "'enc_student_phone', 'enc_student_email', 'enc_student_address', "
                                    + "'enc_student_zip', 'phone_hash_student_pas', 'email_hash_student_pas')")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            entityManager.createNativeQuery(
                            "INSERT INTO customer_auths "
                                    + "(tenant_id, customer_auth_id, provider, token) "
                                    + "VALUES (:tenantId, 1, 'INTERNAL', 'test_token_pas')")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            entityManager.createNativeQuery(
                            "INSERT INTO adult_students "
                                    + "(tenant_id, adult_student_id, customer_auth_id, "
                                    + "birthdate, person_pii_id) "
                                    + "VALUES (:tenantId, :studentId, 1, '1995-07-20', 2)")
                    .setParameter("tenantId", tenantId)
                    .setParameter("studentId", ADULT_STUDENT_ID)
                    .executeUpdate();
        });
    }
}
