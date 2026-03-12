/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.tenancy.TenantDataModel;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for Analytics endpoints.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all 5 analytics REST endpoints return correct structure.
 *
 * <p>Tests use a fresh tenant with no domain data to verify zero-data paths
 * and ensure all native queries execute without errors against a real database.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Analytics — Component Test")
@org.springframework.test.context.TestPropertySource(properties = "spring.cache.type=none")
class AnalyticsComponentTest extends AbstractIntegrationTest {

    private static final String ANALYTICS_BASE = "/v1/analytics";

    private static final String TENANT_ORG_NAME = "Analytics Test Academy";
    private static final String TENANT_EMAIL = "admin@analyticstest.com";
    private static final String TENANT_ADDRESS = "500 Analytics Way";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;

    @BeforeEach
    void setUpTestDataOnce() {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tenantId = createTenant(tx);
        tenantContextHolder.setTenantId(tenantId);
        dataCreated = true;
    }

    // ── Overview ──────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Should return 200 with overview analytics for empty tenant")
    void shouldReturn200_whenOverviewRequested() throws Exception {
        tenantContextHolder.setTenantId(tenantId);

        mockMvc.perform(get(ANALYTICS_BASE + "/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(0))
                .andExpect(jsonPath("$.staffCount").value(0))
                .andExpect(jsonPath("$.revenueMTD").isNumber())
                .andExpect(jsonPath("$.courseUtilization").isNumber())
                .andExpect(jsonPath("$.membershipRenewalRate").isNumber());
    }

    // ── Students ─────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Should return 200 with student analytics for empty tenant")
    void shouldReturn200_whenStudentsRequested() throws Exception {
        tenantContextHolder.setTenantId(tenantId);

        mockMvc.perform(get(ANALYTICS_BASE + "/students")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adultCount").value(0))
                .andExpect(jsonPath("$.minorCount").value(0))
                .andExpect(jsonPath("$.newThisMonth").value(0))
                .andExpect(jsonPath("$.enrollmentTrend").isArray());
    }

    // ── Courses ──────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with course analytics for empty tenant")
    void shouldReturn200_whenCoursesRequested() throws Exception {
        tenantContextHolder.setTenantId(tenantId);

        mockMvc.perform(get(ANALYTICS_BASE + "/courses")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentByCourse").isArray())
                .andExpect(jsonPath("$.capacityUtilization").isArray())
                .andExpect(jsonPath("$.scheduleByDayOfWeek").isArray());
    }

    // ── Staff ────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Should return 200 with staff analytics for empty tenant")
    void shouldReturn200_whenStaffRequested() throws Exception {
        tenantContextHolder.setTenantId(tenantId);

        mockMvc.perform(get(ANALYTICS_BASE + "/staff")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCount").value(0))
                .andExpect(jsonPath("$.tutorCount").value(0))
                .andExpect(jsonPath("$.collaboratorCount").value(0))
                .andExpect(jsonPath("$.distributionByRole").isArray());
    }

    // ── Revenue ──────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Should return 200 with revenue analytics for empty tenant")
    void shouldReturn200_whenRevenueRequested() throws Exception {
        tenantContextHolder.setTenantId(tenantId);

        mockMvc.perform(get(ANALYTICS_BASE + "/revenue")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenueTrend").isArray())
                .andExpect(jsonPath("$.breakdownByType").isArray())
                .andExpect(jsonPath("$.outstandingPayments").isNumber());
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
}
