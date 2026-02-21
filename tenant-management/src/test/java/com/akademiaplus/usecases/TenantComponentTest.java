/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import jakarta.persistence.EntityManager;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for {@code Tenant} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints and exception paths for the Tenant entity.
 *
 * <p>Tenant is unique among domain entities — it uses a simple {@code Long} ID
 * (not composite) and does not require tenant context since it IS the root entity.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Tenant — Component Test")
class TenantComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/tenants";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT1_ORG_NAME = "Test Academy";
    private static final String TENANT1_EMAIL = "admin@testacademy.com";
    private static final String TENANT1_ADDRESS = "456 Education Blvd";
    private static final String TENANT1_LEGAL_NAME = "Test Academy S.A. de C.V.";
    private static final String TENANT1_PHONE = "+525512345678";
    private static final String TENANT1_TAX_ID = "TAC2501011A1";

    private static final String TENANT2_ORG_NAME = "Second Academy";
    private static final String TENANT2_EMAIL = "contact@secondacademy.com";
    private static final String TENANT2_ADDRESS = "789 Learning Lane";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;

    private static Long tenantId;
    private static Long tenant2Id;

    // ── Create Tests ──────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Should return 201 with all fields when given complete request")
    void shouldReturn201WithAllFields_whenGivenCompleteRequest() throws Exception {
        // Given
        String body = """
                {
                    "organization_name": "%s",
                    "email": "%s",
                    "address": "%s",
                    "legal_name": "%s",
                    "phone": "%s",
                    "tax_id": "%s"
                }
                """.formatted(TENANT1_ORG_NAME, TENANT1_EMAIL, TENANT1_ADDRESS,
                TENANT1_LEGAL_NAME, TENANT1_PHONE, TENANT1_TAX_ID);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenant_id").isNumber())
                .andExpect(jsonPath("$.organization_name").value(TENANT1_ORG_NAME))
                .andExpect(jsonPath("$.email").value(TENANT1_EMAIL))
                .andExpect(jsonPath("$.address").value(TENANT1_ADDRESS))
                .andExpect(jsonPath("$.legal_name").value(TENANT1_LEGAL_NAME))
                .andExpect(jsonPath("$.phone").value(TENANT1_PHONE))
                .andExpect(jsonPath("$.tax_id").value(TENANT1_TAX_ID))
                .andReturn();

        // Then
        tenantId = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.tenant_id"))
                .longValue();
        assertThat(tenantId).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 with only required fields")
    void shouldReturn201_whenGivenOnlyRequiredFields() throws Exception {
        // Given
        String body = """
                {
                    "organization_name": "%s",
                    "email": "%s",
                    "address": "%s"
                }
                """.formatted(TENANT2_ORG_NAME, TENANT2_EMAIL, TENANT2_ADDRESS);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenant_id").isNumber())
                .andExpect(jsonPath("$.organization_name").value(TENANT2_ORG_NAME))
                .andExpect(jsonPath("$.email").value(TENANT2_EMAIL))
                .andExpect(jsonPath("$.address").value(TENANT2_ADDRESS))
                .andReturn();

        // Then
        tenant2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.tenant_id"))
                .longValue();
        assertThat(tenant2Id).isPositive();
        assertThat(tenant2Id).isNotEqualTo(tenantId);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with tenant details when found")
    void shouldReturn200_whenTenantFound() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", tenantId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant_id").value(tenantId))
                .andExpect(jsonPath("$.organization_name").value(TENANT1_ORG_NAME))
                .andExpect(jsonPath("$.email").value(TENANT1_EMAIL))
                .andExpect(jsonPath("$.legal_name").value(TENANT1_LEGAL_NAME))
                .andExpect(jsonPath("$.phone").value(TENANT1_PHONE))
                .andExpect(jsonPath("$.tax_id").value(TENANT1_TAX_ID));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when tenant not found")
    void shouldReturn404_whenTenantNotFound() throws Exception {
        // Given
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
    @DisplayName("Should return 200 with list when tenants exist")
    void shouldReturn200WithList_whenTenantsExist() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        assertThat(tenant2Id).isNotNull();

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
    @DisplayName("Should return 404 when deleting non-existent tenant")
    void shouldReturn404_whenDeletingNonExistentTenant() throws Exception {
        // Given
        Long nonExistentId = 999999L;

        // When / Then
        mockMvc.perform(delete(BASE_PATH + "/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    @Test
    @Order(7)
    @DisplayName("Should return 204 when deleting existing tenant")
    void shouldReturn204_whenDeletingExistingTenant() throws Exception {
        // Given
        assertThat(tenant2Id).isNotNull();

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", tenant2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM tenants WHERE tenant_id = :tenantId")
                .setParameter("tenantId", tenant2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("Tenant should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when requesting soft-deleted tenant by ID")
    void shouldReturn404_whenRequestingSoftDeletedTenantById() throws Exception {
        // Given — tenant2Id was soft-deleted
        assertThat(tenant2Id).isNotNull();

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", tenant2Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    @Test
    @Order(9)
    @DisplayName("Should return 409 when deleting tenant with subscriptions")
    void shouldReturn409_whenDeletingTenantWithSubscriptions() throws Exception {
        // Given — tenantId exists, create a subscription for it
        assertThat(tenantId).isNotNull();

        String subscriptionBody = """
                {
                    "type": "basic",
                    "billing_date": "2025-03-01",
                    "rate_per_student": 100.00
                }
                """;
        mockMvc.perform(post("/v1/tenants/{tenantId}/subscriptions", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscriptionBody))
                .andExpect(status().isCreated());

        entityManager.clear();

        // When / Then
        mockMvc.perform(delete(BASE_PATH + "/{id}", tenantId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(
                        BaseControllerAdvice.CODE_DELETION_CONSTRAINT_VIOLATION));
    }
}
