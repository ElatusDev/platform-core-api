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
 * Component test for {@code MembershipTutor} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the MembershipTutor entity.
 *
 * <p>MembershipTutor creation requires existing Membership, Course, and
 * Tutor entities. A Membership is created via the REST endpoint; Course
 * and Tutor prerequisites are inserted directly via native SQL since the
 * billing module does not expose their creation endpoints.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MembershipTutor — Component Test")
class MembershipTutorComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/billing/membershipTutors";
    private static final String MEMBERSHIPS_PATH = "/v1/billing/memberships";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "MembershipTutor Test Academy";
    private static final String TENANT_EMAIL = "admin@mttest.com";
    private static final String TENANT_ADDRESS = "400 MT Blvd";

    // ── Membership test data (prerequisite) ───────────────────────────
    private static final String MEMBERSHIP_TYPE = "ANNUAL";
    private static final double MEMBERSHIP_FEE = 5000.00;
    private static final String MEMBERSHIP_DESCRIPTION = "Annual membership for tutor test";

    // ── MembershipTutor test data ─────────────────────────────────────
    private static final String MT1_START_DATE = "2026-01-01";
    private static final String MT1_DUE_DATE = "2026-12-31";

    private static final String MT2_START_DATE = "2026-03-01";
    private static final String MT2_DUE_DATE = "2026-08-31";

    /**
     * Table names for {@code tenant_sequences} — MembershipTutor creation
     * requires sequences for membership_tutors, memberships, courses,
     * tutors, person_piis, and customer_auths.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "membership_tutors", "memberships", "courses",
            "tutors", "person_piis", "customer_auths",
            "collaborators", "internal_auths"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long membershipId;
    private static final Long COURSE_ID = 1L;
    private static final Long TUTOR_ID = 1L;
    private static Long mt1Id;
    private static Long mt2Id;

    @BeforeEach
    void setUpTestDataOnce() throws Exception {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tenantId = createTenant(tx);
        createTenantSequences(tx);
        createPrerequisiteCourseData(tx);
        createPrerequisiteTutorData(tx);
        tenantContextHolder.setTenantId(tenantId);
        dataCreated = true;
    }

    // ── Setup: Create prerequisite membership ─────────────────────────

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
                .andExpect(jsonPath("$.membershipId").isNumber())
                .andReturn();

        // Then
        membershipId = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.membershipId"))
                .longValue();
        assertThat(membershipId).isPositive();
    }

    // ── Create Tests ──────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Should return 201 when creating membership-tutor with valid references")
    void shouldReturn201_whenCreatingWithValidReferences() throws Exception {
        // Given
        assertThat(membershipId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "startDate": "%s",
                    "dueDate": "%s",
                    "membershipId": %d,
                    "courseId": %d,
                    "tutorId": %d
                }
                """.formatted(MT1_START_DATE, MT1_DUE_DATE,
                membershipId, COURSE_ID, TUTOR_ID);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipTutorId").isNumber())
                .andReturn();

        // Then
        mt1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.membershipTutorId"))
                .longValue();
        assertThat(mt1Id).isPositive();
    }

    @Test
    @Order(3)
    @DisplayName("Should return 201 for second membership-tutor with unique dates")
    void shouldReturn201_whenCreatingSecondMembershipTutor() throws Exception {
        // Given
        assertThat(membershipId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "startDate": "%s",
                    "dueDate": "%s",
                    "membershipId": %d,
                    "courseId": %d,
                    "tutorId": %d
                }
                """.formatted(MT2_START_DATE, MT2_DUE_DATE,
                membershipId, COURSE_ID, TUTOR_ID);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipTutorId").isNumber())
                .andReturn();

        // Then
        mt2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.membershipTutorId"))
                .longValue();
        assertThat(mt2Id).isPositive();
        assertThat(mt2Id).isNotEqualTo(mt1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Should return 200 with details when membership-tutor found")
    void shouldReturn200_whenMembershipTutorFound() throws Exception {
        // Given
        assertThat(mt1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", mt1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipTutorId").value(mt1Id));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 when membership-tutor not found")
    void shouldReturn404_whenMembershipTutorNotFound() throws Exception {
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
    @DisplayName("Should return 200 with list when membership-tutors exist")
    void shouldReturn200WithList_whenMembershipTutorsExist() throws Exception {
        // Given
        assertThat(mt1Id).isNotNull();
        assertThat(mt2Id).isNotNull();
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
    @Order(7)
    @DisplayName("Should return 404 when deleting non-existent membership-tutor")
    void shouldReturn404_whenDeletingNonExistentMembershipTutor() throws Exception {
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
    @DisplayName("Should return 204 when deleting existing membership-tutor")
    void shouldReturn204_whenDeletingExistingMembershipTutor() throws Exception {
        // Given
        assertThat(mt2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", mt2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM membership_tutors " +
                                "WHERE tenant_id = :tenantId AND membership_tutor_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", mt2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("MembershipTutor should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(9)
    @DisplayName("Should return 404 when requesting soft-deleted membership-tutor by ID")
    void shouldReturn404_whenRequestingSoftDeletedMembershipTutorById() throws Exception {
        // Given — mt2Id was soft-deleted
        assertThat(mt2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", mt2Id)
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
     * Creates prerequisite Course data directly via native SQL.
     */
    private void createPrerequisiteCourseData(TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                            "INSERT INTO person_piis "
                                    + "(tenant_id, person_pii_id, encrypted_first_name, encrypted_last_name, "
                                    + "encrypted_phone_number, encrypted_email, encrypted_address, "
                                    + "encrypted_zip_code, phone_number_hash, email_hash) "
                                    + "VALUES (:tenantId, 1, 'enc_first', 'enc_last', "
                                    + "'enc_phone', 'enc_email', 'enc_address', "
                                    + "'enc_zip', 'phone_hash_mt', 'email_hash_mt')")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            entityManager.createNativeQuery(
                            "INSERT INTO internal_auths "
                                    + "(tenant_id, internal_auth_id, encrypted_username, "
                                    + "encrypted_password, encrypted_role, username_hash) "
                                    + "VALUES (:tenantId, 1, 'enc_username', "
                                    + "'enc_password', 'enc_role', 'username_hash_mt')")
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
                                    + "VALUES (:tenantId, :courseId, 'Tutor Test Course', 'Course for MT test', 25)")
                    .setParameter("tenantId", tenantId)
                    .setParameter("courseId", COURSE_ID)
                    .executeUpdate();
        });
    }

    /**
     * Creates prerequisite Tutor data directly via native SQL.
     */
    private void createPrerequisiteTutorData(TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            entityManager.createNativeQuery(
                            "INSERT INTO person_piis "
                                    + "(tenant_id, person_pii_id, encrypted_first_name, encrypted_last_name, "
                                    + "encrypted_phone_number, encrypted_email, encrypted_address, "
                                    + "encrypted_zip_code, phone_number_hash, email_hash) "
                                    + "VALUES (:tenantId, 2, 'enc_tutor_first', 'enc_tutor_last', "
                                    + "'enc_tutor_phone', 'enc_tutor_email', 'enc_tutor_address', "
                                    + "'enc_tutor_zip', 'phone_hash_tutor_mt', 'email_hash_tutor_mt')")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            entityManager.createNativeQuery(
                            "INSERT INTO customer_auths "
                                    + "(tenant_id, customer_auth_id, provider, token) "
                                    + "VALUES (:tenantId, 1, 'INTERNAL', 'test_token_tutor_mt')")
                    .setParameter("tenantId", tenantId)
                    .executeUpdate();

            entityManager.createNativeQuery(
                            "INSERT INTO tutors "
                                    + "(tenant_id, tutor_id, customer_auth_id, "
                                    + "birthdate, person_pii_id) "
                                    + "VALUES (:tenantId, :tutorId, 1, '1985-05-20', 2)")
                    .setParameter("tenantId", tenantId)
                    .setParameter("tutorId", TUTOR_ID)
                    .executeUpdate();
        });
    }
}
