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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for {@code TenantBranding} GET and PUT operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies the REST endpoints for the TenantBranding entity.
 *
 * <p>TenantBranding has no auto-generated ID; it uses tenantId as its sole PK.
 * No tenant_sequences row is needed for this entity.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TenantBranding — Component Test")
class TenantBrandingComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/tenant/branding";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Branding Test Academy";
    private static final String TENANT_EMAIL = "admin@brandingtest.com";
    private static final String TENANT_ADDRESS = "600 Branding Lane";

    // ── Branding test data ────────────────────────────────────────────
    private static final String SCHOOL_NAME = "Bright Future Academy";
    private static final String PRIMARY_COLOR = "#FF5733";
    private static final String SECONDARY_COLOR = "#C70039";
    private static final String LOGO_URL = "https://cdn.example.com/logo.png";
    private static final String FONT_FAMILY = "Roboto";

    private static final String UPDATED_SCHOOL_NAME = "Bright Future Academy v2";
    private static final String UPDATED_PRIMARY_COLOR = "#1976D2";

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

    // ── GET Default Tests ─────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Should return 200 with default branding when no custom branding exists")
    void shouldReturn200WithDefaults_whenNoCustomBrandingExists() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolName").value("My School"))
                .andExpect(jsonPath("$.primaryColor").value("#1976D2"))
                .andExpect(jsonPath("$.secondaryColor").value("#FF9800"));
    }

    // ── PUT (Create) Tests ────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Should return 200 when creating branding via upsert")
    void shouldReturn200_whenCreatingBrandingViaUpsert() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "schoolName": "%s",
                    "primaryColor": "%s",
                    "secondaryColor": "%s",
                    "logoUrl": "%s",
                    "fontFamily": "%s"
                }
                """.formatted(
                SCHOOL_NAME, PRIMARY_COLOR, SECONDARY_COLOR, LOGO_URL, FONT_FAMILY);

        // When / Then
        mockMvc.perform(put(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolName").value(SCHOOL_NAME))
                .andExpect(jsonPath("$.primaryColor").value(PRIMARY_COLOR))
                .andExpect(jsonPath("$.secondaryColor").value(SECONDARY_COLOR))
                .andExpect(jsonPath("$.logoUrl").value(LOGO_URL))
                .andExpect(jsonPath("$.fontFamily").value(FONT_FAMILY));
    }

    // ── GET (After Create) Tests ──────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with saved branding after upsert")
    void shouldReturn200WithSavedBranding_afterUpsert() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolName").value(SCHOOL_NAME))
                .andExpect(jsonPath("$.primaryColor").value(PRIMARY_COLOR))
                .andExpect(jsonPath("$.secondaryColor").value(SECONDARY_COLOR))
                .andExpect(jsonPath("$.logoUrl").value(LOGO_URL))
                .andExpect(jsonPath("$.fontFamily").value(FONT_FAMILY));
    }

    // ── PUT (Update) Tests ────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Should return 200 when updating existing branding")
    void shouldReturn200_whenUpdatingExistingBranding() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "schoolName": "%s",
                    "primaryColor": "%s",
                    "secondaryColor": "%s"
                }
                """.formatted(
                UPDATED_SCHOOL_NAME, UPDATED_PRIMARY_COLOR, SECONDARY_COLOR);

        // When / Then
        mockMvc.perform(put(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolName").value(UPDATED_SCHOOL_NAME))
                .andExpect(jsonPath("$.primaryColor").value(UPDATED_PRIMARY_COLOR))
                .andExpect(jsonPath("$.secondaryColor").value(SECONDARY_COLOR));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 200 with updated values on subsequent GET")
    void shouldReturn200WithUpdatedValues_onSubsequentGet() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolName").value(UPDATED_SCHOOL_NAME))
                .andExpect(jsonPath("$.primaryColor").value(UPDATED_PRIMARY_COLOR));
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
