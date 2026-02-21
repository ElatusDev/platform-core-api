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
 * Component test for {@code Membership} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the Membership entity.
 *
 * <p>Membership is a root entity with no foreign key dependencies.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Membership — Component Test")
class MembershipComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/billing/memberships";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Membership Test Academy";
    private static final String TENANT_EMAIL = "admin@membershiptest.com";
    private static final String TENANT_ADDRESS = "100 Membership Blvd";

    // ── Membership test data ──────────────────────────────────────────
    private static final String MEMBERSHIP1_TYPE = "MONTHLY";
    private static final double MEMBERSHIP1_FEE = 500.00;
    private static final String MEMBERSHIP1_DESCRIPTION = "Monthly basic membership";

    private static final String MEMBERSHIP2_TYPE = "ANNUAL";
    private static final double MEMBERSHIP2_FEE = 5000.00;
    private static final String MEMBERSHIP2_DESCRIPTION = "Annual premium membership";

    /**
     * Table names for {@code tenant_sequences} — Membership creation
     * requires a sequence for memberships.
     */
    private static final String[] ENTITY_TABLE_NAMES = {"memberships"};

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long membership1Id;
    private static Long membership2Id;

    @BeforeEach
    void setUpTestDataOnce() {
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
    @DisplayName("Should return 201 when creating membership with required fields")
    void shouldReturn201_whenCreatingMembershipWithRequiredFields() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "membershipType": "%s",
                    "fee": %s,
                    "description": "%s"
                }
                """.formatted(MEMBERSHIP1_TYPE, MEMBERSHIP1_FEE, MEMBERSHIP1_DESCRIPTION);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipId").isNumber())
                .andReturn();

        // Then
        membership1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.membershipId"))
                .longValue();
        assertThat(membership1Id).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 for second membership with unique data")
    void shouldReturn201_whenCreatingSecondMembership() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "membershipType": "%s",
                    "fee": %s,
                    "description": "%s"
                }
                """.formatted(MEMBERSHIP2_TYPE, MEMBERSHIP2_FEE, MEMBERSHIP2_DESCRIPTION);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipId").isNumber())
                .andReturn();

        // Then
        membership2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.membershipId"))
                .longValue();
        assertThat(membership2Id).isPositive();
        assertThat(membership2Id).isNotEqualTo(membership1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with membership details when found")
    void shouldReturn200_whenMembershipFound() throws Exception {
        // Given
        assertThat(membership1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", membership1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(membership1Id))
                .andExpect(jsonPath("$.membershipType").value(MEMBERSHIP1_TYPE))
                .andExpect(jsonPath("$.description").value(MEMBERSHIP1_DESCRIPTION));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when membership not found")
    void shouldReturn404_whenMembershipNotFound() throws Exception {
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
    @DisplayName("Should return 200 with list when memberships exist")
    void shouldReturn200WithList_whenMembershipsExist() throws Exception {
        // Given
        assertThat(membership1Id).isNotNull();
        assertThat(membership2Id).isNotNull();
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
    @DisplayName("Should return 404 when deleting non-existent membership")
    void shouldReturn404_whenDeletingNonExistentMembership() throws Exception {
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
    @DisplayName("Should return 204 when deleting existing membership")
    void shouldReturn204_whenDeletingExistingMembership() throws Exception {
        // Given
        assertThat(membership2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", membership2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM memberships " +
                                "WHERE tenant_id = :tenantId AND membership_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", membership2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("Membership should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when requesting soft-deleted membership by ID")
    void shouldReturn404_whenRequestingSoftDeletedMembershipById() throws Exception {
        // Given — membership2Id was soft-deleted
        assertThat(membership2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", membership2Id)
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
