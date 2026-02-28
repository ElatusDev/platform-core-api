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
 * Component test for {@code TenantBillingCycle} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the TenantBillingCycle entity.
 *
 * <p>TenantBillingCycle uses composite keys (tenantId + tenantBillingCycleId) and is
 * tenant-scoped, requiring a parent tenant to exist. Billing cycles do NOT have a
 * DELETE endpoint — status is updated via PATCH instead.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TenantBillingCycle — Component Test")
class TenantBillingCycleComponentTest extends AbstractIntegrationTest {

    private static final String TENANT_PATH = "/v1/tenants";
    private static final String BILLING_CYCLE_PATH_TEMPLATE = "/v1/tenants/{tenantId}/billing-cycles";
    private static final String BILLING_CYCLE_BY_ID_PATH_TEMPLATE = "/v1/tenants/{tenantId}/billing-cycles/{billingCycleId}";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Billing Cycle Test Academy";
    private static final String TENANT_EMAIL = "billing@cycletest.com";
    private static final String TENANT_ADDRESS = "456 Billing Avenue";

    // ── Billing cycle test data ───────────────────────────────────────
    private static final String BILLING_CYCLE1_MONTH = "2025-03";
    private static final String BILLING_CYCLE1_CALC_DATE = "2025-03-01";
    private static final Integer BILLING_CYCLE1_USER_COUNT = 150;
    private static final String BILLING_CYCLE1_TOTAL_AMOUNT = "15000.00";
    private static final String BILLING_CYCLE1_NOTES = "First billing cycle test";

    private static final String BILLING_CYCLE2_MONTH = "2025-04";
    private static final String BILLING_CYCLE2_CALC_DATE = "2025-04-01";
    private static final Integer BILLING_CYCLE2_USER_COUNT = 175;
    private static final String BILLING_CYCLE2_TOTAL_AMOUNT = "17500.00";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;

    private static Long tenantId;
    private static Long billingCycle1Id;
    private static Long billingCycle2Id;

    // ── Setup: Create parent tenant ──────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Setup: Should create parent tenant for billing cycle tests")
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
                    "billing_month": "%s",
                    "calculation_date": "%s",
                    "user_count": %d,
                    "total_amount": %s,
                    "notes": "%s"
                }
                """.formatted(BILLING_CYCLE1_MONTH, BILLING_CYCLE1_CALC_DATE,
                BILLING_CYCLE1_USER_COUNT, BILLING_CYCLE1_TOTAL_AMOUNT, BILLING_CYCLE1_NOTES);

        // When
        MvcResult result = mockMvc.perform(post(BILLING_CYCLE_PATH_TEMPLATE, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenant_billing_cycle_id").isNumber())
                .andExpect(jsonPath("$.tenant_id").value(tenantId))
                .andExpect(jsonPath("$.billing_month").value(BILLING_CYCLE1_MONTH + "-01"))
                .andExpect(jsonPath("$.calculation_date").value(BILLING_CYCLE1_CALC_DATE))
                .andExpect(jsonPath("$.user_count").value(BILLING_CYCLE1_USER_COUNT))
                .andExpect(jsonPath("$.total_amount").value(Double.parseDouble(BILLING_CYCLE1_TOTAL_AMOUNT)))
                .andExpect(jsonPath("$.billing_status").value("PENDING"))
                .andExpect(jsonPath("$.notes").value(BILLING_CYCLE1_NOTES))
                .andReturn();

        // Then
        billingCycle1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.tenant_billing_cycle_id"))
                .longValue();
        assertThat(billingCycle1Id).isPositive();
    }

    @Test
    @Order(3)
    @DisplayName("Should return 201 with only required fields")
    void shouldReturn201_whenGivenOnlyRequiredFields() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        String body = """
                {
                    "billing_month": "%s",
                    "calculation_date": "%s",
                    "user_count": %d,
                    "total_amount": %s
                }
                """.formatted(BILLING_CYCLE2_MONTH, BILLING_CYCLE2_CALC_DATE,
                BILLING_CYCLE2_USER_COUNT, BILLING_CYCLE2_TOTAL_AMOUNT);

        // When
        MvcResult result = mockMvc.perform(post(BILLING_CYCLE_PATH_TEMPLATE, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenant_billing_cycle_id").isNumber())
                .andExpect(jsonPath("$.tenant_id").value(tenantId))
                .andExpect(jsonPath("$.billing_month").value(BILLING_CYCLE2_MONTH + "-01"))
                .andExpect(jsonPath("$.calculation_date").value(BILLING_CYCLE2_CALC_DATE))
                .andExpect(jsonPath("$.user_count").value(BILLING_CYCLE2_USER_COUNT))
                .andExpect(jsonPath("$.total_amount").value(Double.parseDouble(BILLING_CYCLE2_TOTAL_AMOUNT)))
                .andExpect(jsonPath("$.billing_status").value("PENDING"))
                .andReturn();

        // Then
        billingCycle2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.tenant_billing_cycle_id"))
                .longValue();
        assertThat(billingCycle2Id).isPositive();
        assertThat(billingCycle2Id).isNotEqualTo(billingCycle1Id);
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when creating billing cycle for non-existent tenant")
    void shouldReturn404_whenCreatingBillingCycleForNonExistentTenant() throws Exception {
        // Given
        Long nonExistentTenantId = 999999L;
        String body = """
                {
                    "billing_month": "2025-05",
                    "calculation_date": "2025-05-01",
                    "user_count": 100,
                    "total_amount": 10000.00
                }
                """;

        // When / Then
        mockMvc.perform(post(BILLING_CYCLE_PATH_TEMPLATE, nonExistentTenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Should return 200 with billing cycle details when found")
    void shouldReturn200_whenBillingCycleFound() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        assertThat(billingCycle1Id).isNotNull();

        // When / Then
        mockMvc.perform(get(BILLING_CYCLE_BY_ID_PATH_TEMPLATE, tenantId, billingCycle1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant_billing_cycle_id").value(billingCycle1Id))
                .andExpect(jsonPath("$.tenant_id").value(tenantId))
                .andExpect(jsonPath("$.billing_month").value(BILLING_CYCLE1_MONTH + "-01"))
                .andExpect(jsonPath("$.calculation_date").value(BILLING_CYCLE1_CALC_DATE))
                .andExpect(jsonPath("$.user_count").value(BILLING_CYCLE1_USER_COUNT))
                .andExpect(jsonPath("$.total_amount").value(Double.parseDouble(BILLING_CYCLE1_TOTAL_AMOUNT)))
                .andExpect(jsonPath("$.notes").value(BILLING_CYCLE1_NOTES));
    }

    @Test
    @Order(6)
    @DisplayName("Should return 404 when billing cycle not found")
    void shouldReturn404_whenBillingCycleNotFound() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        Long nonExistentBillingCycleId = 999999L;

        // When / Then
        mockMvc.perform(get(BILLING_CYCLE_BY_ID_PATH_TEMPLATE, tenantId, nonExistentBillingCycleId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── GetAll Tests ──────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("Should return 200 with list when billing cycles exist")
    void shouldReturn200WithList_whenBillingCyclesExist() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        assertThat(billingCycle1Id).isNotNull();
        assertThat(billingCycle2Id).isNotNull();

        // When / Then
        mockMvc.perform(get(BILLING_CYCLE_PATH_TEMPLATE, tenantId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when listing billing cycles for non-existent tenant")
    void shouldReturn404_whenListingBillingCyclesForNonExistentTenant() throws Exception {
        // Given
        Long nonExistentTenantId = 999999L;

        // When / Then
        mockMvc.perform(get(BILLING_CYCLE_PATH_TEMPLATE, nonExistentTenantId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── Update Status Tests ───────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("Should return 200 when updating billing cycle status")
    void shouldReturn200_whenUpdatingBillingCycleStatus() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        assertThat(billingCycle1Id).isNotNull();
        String body = """
                {
                    "status": "billed",
                    "notes": "Invoice sent to client"
                }
                """;

        // When
        mockMvc.perform(patch(BILLING_CYCLE_BY_ID_PATH_TEMPLATE, tenantId, billingCycle1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant_billing_cycle_id").value(billingCycle1Id))
                .andExpect(jsonPath("$.billing_status").value("BILLED"))
                .andExpect(jsonPath("$.billed_at").isNotEmpty());

        // Then — verify status updated in database
        entityManager.clear();
        String status = (String) entityManager
                .createNativeQuery(
                        "SELECT billing_status FROM tenant_billing_cycles " +
                                "WHERE tenant_id = :tenantId AND tenant_billing_cycle_id = :cycleId")
                .setParameter("tenantId", tenantId)
                .setParameter("cycleId", billingCycle1Id)
                .getSingleResult();
        assertThat(status).isEqualTo("BILLED");
    }

    @Test
    @Order(10)
    @DisplayName("Should return 404 when updating non-existent billing cycle status")
    void shouldReturn404_whenUpdatingNonExistentBillingCycleStatus() throws Exception {
        // Given
        assertThat(tenantId).isNotNull();
        Long nonExistentBillingCycleId = 999999L;
        String body = """
                {
                    "status": "paid"
                }
                """;

        // When / Then
        mockMvc.perform(patch(BILLING_CYCLE_BY_ID_PATH_TEMPLATE, tenantId, nonExistentBillingCycleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }
}
