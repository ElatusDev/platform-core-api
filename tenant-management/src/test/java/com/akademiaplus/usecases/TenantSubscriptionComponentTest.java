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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for {@code TenantSubscription} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints and exception paths for the TenantSubscription entity.
 *
 * <p>TenantSubscription uses composite keys (tenantId + tenantSubscriptionId) and is
 * tenant-scoped, requiring a parent tenant to exist.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TenantSubscription — Component Test")
class TenantSubscriptionComponentTest extends AbstractIntegrationTest {

    private static final String TENANT_PATH = "/v1/tenants";
    private static final String SUBSCRIPTION_PATH_TEMPLATE = "/v1/tenants/{tenantId}/subscriptions";
    private static final String SUBSCRIPTION_BY_ID_PATH_TEMPLATE = "/v1/tenants/{tenantId}/subscriptions/{subscriptionId}";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Subscription Test Academy";
    private static final String TENANT_EMAIL = "billing@subscriptiontest.com";
    private static final String TENANT_ADDRESS = "123 Billing Street";

    // ── Subscription test data ────────────────────────────────────────
    private static final String SUBSCRIPTION1_TYPE = "basic";
    private static final Integer SUBSCRIPTION1_MAX_USERS = 50;
    private static final String SUBSCRIPTION1_BILLING_DATE = "2025-03-01";
    private static final String SUBSCRIPTION1_RATE = "100.00";

    private static final String SUBSCRIPTION2_TYPE = "premium";
    private static final String SUBSCRIPTION2_BILLING_DATE = "2025-04-01";
    private static final String SUBSCRIPTION2_RATE = "250.00";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;

    private static Long tenantId;
    private static Long subscription1Id;
    private static Long subscription2Id;

    // ── Setup: Create parent tenant ──────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Setup: Should create parent tenant for subscription tests")
    void setup_shouldCreateParentTenant() throws Exception {
        // Given
        String body = """
                {
                    "organization_name": "%s",
                    "email": "%s",
                    "address": "%s"
                }
                """.formatted(TENANT_ORG_NAME, TENANT_EMAIL, TENANT_ADDRESS);

        // When
        MvcResult result = mockMvc.perform(post(TENANT_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenant_id").isNumber())
                .andReturn();

        // Then
        tenantId = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.tenant_id"))
                .longValue();
        assertThat(tenantId).isPositive();
    }

    // ── Create Tests ──────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Should return 201 with all fields when given complete request")
    void shouldReturn201WithAllFields_whenGivenCompleteRequest() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        String body = """
                {
                    "type": "%s",
                    "max_users": %d,
                    "billing_date": "%s",
                    "rate_per_student": %s
                }
                """.formatted(SUBSCRIPTION1_TYPE, SUBSCRIPTION1_MAX_USERS,
                SUBSCRIPTION1_BILLING_DATE, SUBSCRIPTION1_RATE);

        // When
        MvcResult result = mockMvc.perform(post(SUBSCRIPTION_PATH_TEMPLATE, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenant_subscription_id").isNumber())
                .andExpect(jsonPath("$.tenant_id").value(tenantId))
                .andExpect(jsonPath("$.type").value(SUBSCRIPTION1_TYPE))
                .andExpect(jsonPath("$.max_users").value(SUBSCRIPTION1_MAX_USERS))
                .andExpect(jsonPath("$.billing_date").value(SUBSCRIPTION1_BILLING_DATE))
                .andExpect(jsonPath("$.rate_per_student").value(Double.parseDouble(SUBSCRIPTION1_RATE)))
                .andReturn();

        // Then
        subscription1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.tenant_subscription_id"))
                .longValue();
        assertThat(subscription1Id).isPositive();
    }

    @Test
    @Order(3)
    @DisplayName("Should return 201 with only required fields")
    void shouldReturn201_whenGivenOnlyRequiredFields() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        String body = """
                {
                    "type": "%s",
                    "billing_date": "%s",
                    "rate_per_student": %s
                }
                """.formatted(SUBSCRIPTION2_TYPE, SUBSCRIPTION2_BILLING_DATE, SUBSCRIPTION2_RATE);

        // When
        MvcResult result = mockMvc.perform(post(SUBSCRIPTION_PATH_TEMPLATE, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenant_subscription_id").isNumber())
                .andExpect(jsonPath("$.tenant_id").value(tenantId))
                .andExpect(jsonPath("$.type").value(SUBSCRIPTION2_TYPE))
                .andExpect(jsonPath("$.max_users").isEmpty())
                .andExpect(jsonPath("$.billing_date").value(SUBSCRIPTION2_BILLING_DATE))
                .andExpect(jsonPath("$.rate_per_student").value(Double.parseDouble(SUBSCRIPTION2_RATE)))
                .andReturn();

        // Then
        subscription2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.tenant_subscription_id"))
                .longValue();
        assertThat(subscription2Id).isPositive();
        assertThat(subscription2Id).isNotEqualTo(subscription1Id);
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when creating subscription for non-existent tenant")
    void shouldReturn404_whenCreatingSubscriptionForNonExistentTenant() throws Exception {
        // Given
        Long nonExistentTenantId = 999999L;
        String body = """
                {
                    "type": "basic",
                    "billing_date": "2025-03-01",
                    "rate_per_student": 100.00
                }
                """;

        // When / Then
        mockMvc.perform(post(SUBSCRIPTION_PATH_TEMPLATE, nonExistentTenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Should return 200 with subscription details when found")
    void shouldReturn200_whenSubscriptionFound() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        assertThat(subscription1Id).isNotNull();

        // When / Then
        mockMvc.perform(get(SUBSCRIPTION_BY_ID_PATH_TEMPLATE, tenantId, subscription1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant_subscription_id").value(subscription1Id))
                .andExpect(jsonPath("$.tenant_id").value(tenantId))
                .andExpect(jsonPath("$.type").value(SUBSCRIPTION1_TYPE))
                .andExpect(jsonPath("$.max_users").value(SUBSCRIPTION1_MAX_USERS))
                .andExpect(jsonPath("$.billing_date").value(SUBSCRIPTION1_BILLING_DATE))
                .andExpect(jsonPath("$.rate_per_student").value(Double.parseDouble(SUBSCRIPTION1_RATE)));
    }

    @Test
    @Order(6)
    @DisplayName("Should return 404 when subscription not found")
    void shouldReturn404_whenSubscriptionNotFound() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        Long nonExistentSubscriptionId = 999999L;

        // When / Then
        mockMvc.perform(get(SUBSCRIPTION_BY_ID_PATH_TEMPLATE, tenantId, nonExistentSubscriptionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── GetAll Tests ──────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("Should return 200 with list when subscriptions exist")
    void shouldReturn200WithList_whenSubscriptionsExist() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        assertThat(subscription1Id).isNotNull();
        assertThat(subscription2Id).isNotNull();

        // When / Then
        mockMvc.perform(get(SUBSCRIPTION_PATH_TEMPLATE, tenantId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when listing subscriptions for non-existent tenant")
    void shouldReturn404_whenListingSubscriptionsForNonExistentTenant() throws Exception {
        // Given
        Long nonExistentTenantId = 999999L;

        // When / Then
        mockMvc.perform(get(SUBSCRIPTION_PATH_TEMPLATE, nonExistentTenantId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── Delete Tests ──────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("Should return 404 when deleting non-existent subscription")
    void shouldReturn404_whenDeletingNonExistentSubscription() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        Long nonExistentSubscriptionId = 999999L;

        // When / Then
        mockMvc.perform(delete(SUBSCRIPTION_BY_ID_PATH_TEMPLATE, tenantId, nonExistentSubscriptionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    @Test
    @Order(10)
    @DisplayName("Should return 204 when deleting existing subscription")
    void shouldReturn204_whenDeletingExistingSubscription() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        assertThat(subscription2Id).isNotNull();

        // When
        mockMvc.perform(delete(SUBSCRIPTION_BY_ID_PATH_TEMPLATE, tenantId, subscription2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM tenant_subscriptions " +
                                "WHERE tenant_id = :tenantId AND tenant_subscription_id = :subscriptionId")
                .setParameter("tenantId", tenantId)
                .setParameter("subscriptionId", subscription2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("Subscription should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(11)
    @DisplayName("Should return 404 when requesting soft-deleted subscription by ID")
    void shouldReturn404_whenRequestingSoftDeletedSubscriptionById() throws Exception {
        // Given — subscription2Id was soft-deleted
        assertThat(tenantId).isNotNull();
        assertThat(subscription2Id).isNotNull();

        // When / Then
        mockMvc.perform(get(SUBSCRIPTION_BY_ID_PATH_TEMPLATE, tenantId, subscription2Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }
}
