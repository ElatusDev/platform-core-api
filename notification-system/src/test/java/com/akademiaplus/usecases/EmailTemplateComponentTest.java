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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for email template CRUD and preview operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for email template management including
 * creation, listing, retrieval, update, and template preview rendering.
 *
 * <p>{@link JavaMailSender} is mocked via {@code @MockitoBean} to prevent
 * real SMTP connections during testing.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("EmailTemplate — Component Test")
class EmailTemplateComponentTest extends AbstractIntegrationTest {

    private static final String TEMPLATE_BASE_PATH = "/v1/notification-system/email/templates";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Template Test Academy";
    private static final String TENANT_EMAIL = "admin@templatetest.com";
    private static final String TENANT_ADDRESS = "500 Template Ave";

    // ── Template test data ────────────────────────────────────────────
    private static final String TEMPLATE_NAME = "Welcome";
    private static final String TEMPLATE_DESCRIPTION = "Welcome email";
    private static final String TEMPLATE_CATEGORY = "onboarding";
    private static final String TEMPLATE_SUBJECT = "Welcome {{name}}";
    private static final String TEMPLATE_BODY_HTML = "<h1>Hi {{name}}</h1>";
    private static final Boolean TEMPLATE_IS_ACTIVE = true;

    // ── Variable test data ────────────────────────────────────────────
    private static final String VARIABLE_NAME = "name";
    private static final String VARIABLE_TYPE = "STRING";
    private static final Boolean VARIABLE_REQUIRED = true;

    // ── Update test data ──────────────────────────────────────────────
    private static final String UPDATED_TEMPLATE_NAME = "Welcome Updated";

    // ── Preview test data ─────────────────────────────────────────────
    private static final String PREVIEW_NAME_VALUE = "John";
    private static final String PREVIEW_EXPECTED_SUBJECT = "Welcome John";

    /**
     * Table names for {@code tenant_sequences} — Email template operations
     * require sequences for email_templates and email_template_variables.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "email_templates", "email_template_variables"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long templateId;

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

    // ── Template Lifecycle Tests ─────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Should return 201 when creating template")
    void shouldReturn201_whenCreatingTemplate() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "name": "%s",
                    "description": "%s",
                    "category": "%s",
                    "subject": "%s",
                    "bodyHtml": "%s",
                    "isActive": %s,
                    "variables": [
                        {
                            "name": "%s",
                            "type": "%s",
                            "required": %s
                        }
                    ]
                }
                """.formatted(
                TEMPLATE_NAME,
                TEMPLATE_DESCRIPTION,
                TEMPLATE_CATEGORY,
                TEMPLATE_SUBJECT,
                TEMPLATE_BODY_HTML,
                TEMPLATE_IS_ACTIVE,
                VARIABLE_NAME,
                VARIABLE_TYPE,
                VARIABLE_REQUIRED);

        // When
        MvcResult result = mockMvc.perform(post(TEMPLATE_BASE_PATH)
                        .header(TENANT_HEADER, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateId").exists())
                .andExpect(jsonPath("$.name").value(TEMPLATE_NAME))
                .andReturn();

        // Then
        Object rawTemplateId = com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.templateId");
        if (rawTemplateId instanceof Number number) {
            templateId = number.longValue();
        } else {
            templateId = Long.parseLong(rawTemplateId.toString());
        }
        assertThat(templateId).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 200 when listing templates")
    void shouldReturn200_whenListingTemplates() throws Exception {
        // Given
        assertThat(templateId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(TEMPLATE_BASE_PATH)
                        .header(TENANT_HEADER, tenantId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templates").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("Should return 200 when getting template by ID")
    void shouldReturn200_whenGettingTemplateById() throws Exception {
        // Given
        assertThat(templateId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(TEMPLATE_BASE_PATH + "/{templateId}", templateId)
                        .header(TENANT_HEADER, tenantId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(TEMPLATE_NAME));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 200 when updating template")
    void shouldReturn200_whenUpdatingTemplate() throws Exception {
        // Given
        assertThat(templateId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "name": "%s",
                    "description": "%s",
                    "category": "%s",
                    "subject": "%s",
                    "bodyHtml": "%s",
                    "isActive": %s,
                    "variables": [
                        {
                            "name": "%s",
                            "type": "%s",
                            "required": %s
                        }
                    ]
                }
                """.formatted(
                UPDATED_TEMPLATE_NAME,
                TEMPLATE_DESCRIPTION,
                TEMPLATE_CATEGORY,
                TEMPLATE_SUBJECT,
                TEMPLATE_BODY_HTML,
                TEMPLATE_IS_ACTIVE,
                VARIABLE_NAME,
                VARIABLE_TYPE,
                VARIABLE_REQUIRED);

        // When / Then
        mockMvc.perform(put(TEMPLATE_BASE_PATH + "/{templateId}", templateId)
                        .header(TENANT_HEADER, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(UPDATED_TEMPLATE_NAME));
    }

    // ── Template Preview Tests ───────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Should return 200 when previewing template")
    void shouldReturn200_whenPreviewingTemplate() throws Exception {
        // Given
        assertThat(templateId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "templateData": {
                        "name": "%s"
                    }
                }
                """.formatted(PREVIEW_NAME_VALUE);

        // When / Then
        mockMvc.perform(post(TEMPLATE_BASE_PATH + "/{templateId}/preview", templateId)
                        .header(TENANT_HEADER, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value(PREVIEW_EXPECTED_SUBJECT));
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
