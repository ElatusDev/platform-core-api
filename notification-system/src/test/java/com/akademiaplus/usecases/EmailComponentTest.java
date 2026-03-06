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
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for email notification operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for email immediate send and email delivery
 * lifecycle management.
 *
 * <p>{@link JavaMailSender} is mocked via {@code @MockitoBean} to prevent
 * real SMTP connections during testing.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Email — Component Test")
class EmailComponentTest extends AbstractIntegrationTest {

    private static final String EMAIL_BASE_PATH = "/v1/notification-system/email";
    private static final String NOTIFICATION_BASE_PATH = "/v1/notification-system/notifications";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Email Test Academy";
    private static final String TENANT_EMAIL = "admin@emailtest.com";
    private static final String TENANT_ADDRESS = "400 Email Ave";

    // ── Immediate send test data ──────────────────────────────────────
    private static final String IMMEDIATE_SUBJECT = "Test";
    private static final String IMMEDIATE_BODY = "<p>Hello</p>";
    private static final String IMMEDIATE_RECIPIENT_1 = "user@test.com";
    private static final String IMMEDIATE_RECIPIENT_2 = "user2@test.com";
    private static final String IMMEDIATE_MESSAGE = "Email delivery completed";

    // ── Notification test data ────────────────────────────────────────
    private static final String NOTIFICATION_TITLE = "Email Delivery Test";
    private static final String NOTIFICATION_CONTENT = "Content for email delivery";
    private static final String NOTIFICATION_TYPE = "SYSTEM_MAINTENANCE";
    private static final String NOTIFICATION_PRIORITY = "HIGH";
    private static final Long NOTIFICATION_TARGET_USER_ID = 100L;

    // ── Delivery test data ────────────────────────────────────────────
    private static final String DELIVERY_RECIPIENT = "recipient@test.com";

    /**
     * Table names for {@code tenant_sequences} — Email operations require
     * sequences for notifications, notification_deliveries, emails,
     * email_recipients, and email_attachments.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "notifications", "notification_deliveries", "emails",
            "email_recipients", "email_attachments"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long notificationId;
    private static Long deliveryId;

    @BeforeEach
    void setUpTestDataOnce() {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tenantId = createTenant(tx);
        createTenantSequences(tx);
        tenantContextHolder.setTenantId(tenantId);

        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        dataCreated = true;
    }

    // ── Immediate Send Tests ─────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Should return 200 when immediate send succeeds")
    void shouldReturn200_whenImmediateSendSucceeds() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "subject": "%s",
                    "body": "%s",
                    "recipients": {
                        "to": ["%s"]
                    }
                }
                """.formatted(IMMEDIATE_SUBJECT, IMMEDIATE_BODY, IMMEDIATE_RECIPIENT_1);

        // When / Then
        mockMvc.perform(post(EMAIL_BASE_PATH + "/send")
                        .header(TENANT_HEADER, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(IMMEDIATE_MESSAGE));
    }

    @Test
    @Order(2)
    @DisplayName("Should return 200 when immediate send with multiple recipients")
    void shouldReturn200_whenImmediateSendWithMultipleRecipients() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "subject": "%s",
                    "body": "%s",
                    "recipients": {
                        "to": ["%s", "%s"]
                    }
                }
                """.formatted(
                IMMEDIATE_SUBJECT,
                IMMEDIATE_BODY,
                IMMEDIATE_RECIPIENT_1,
                IMMEDIATE_RECIPIENT_2);

        // When / Then
        mockMvc.perform(post(EMAIL_BASE_PATH + "/send")
                        .header(TENANT_HEADER, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryResults.length()").value(2));
    }

    // ── Email Delivery Lifecycle Tests ───────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 201 when creating email delivery")
    void shouldReturn201_whenCreatingEmailDelivery() throws Exception {
        // Given — first create a notification
        tenantContextHolder.setTenantId(tenantId);
        String notificationBody = """
                {
                    "title": "%s",
                    "content": "%s",
                    "type": "%s",
                    "priority": "%s",
                    "targetUserId": %d
                }
                """.formatted(
                NOTIFICATION_TITLE,
                NOTIFICATION_CONTENT,
                NOTIFICATION_TYPE,
                NOTIFICATION_PRIORITY,
                NOTIFICATION_TARGET_USER_ID);

        MvcResult notificationResult = mockMvc.perform(post(NOTIFICATION_BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificationBody))
                .andExpect(status().isCreated())
                .andReturn();

        notificationId = ((Number) com.jayway.jsonpath.JsonPath
                .read(notificationResult.getResponse().getContentAsString(), "$.notificationId"))
                .longValue();

        // Given — build delivery request
        String deliveryBody = """
                {
                    "recipients": {
                        "to": ["%s"]
                    },
                    "config": {}
                }
                """.formatted(DELIVERY_RECIPIENT);

        // When
        MvcResult result = mockMvc.perform(post(EMAIL_BASE_PATH
                        + "/notifications/{notificationId}/deliveries", notificationId)
                        .header(TENANT_HEADER, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryId").isNumber())
                .andReturn();

        // Then
        deliveryId = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.deliveryId"))
                .longValue();
        assertThat(deliveryId).isPositive();
    }

    @Test
    @Order(4)
    @DisplayName("Should return 200 when listing email deliveries")
    void shouldReturn200_whenListingEmailDeliveries() throws Exception {
        // Given
        assertThat(notificationId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(EMAIL_BASE_PATH
                        + "/notifications/{notificationId}/deliveries", notificationId)
                        .header(TENANT_HEADER, tenantId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveries").isArray());
    }

    @Test
    @Order(5)
    @DisplayName("Should return 200 when getting delivery by ID")
    void shouldReturn200_whenGettingDeliveryById() throws Exception {
        // Given
        assertThat(deliveryId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(EMAIL_BASE_PATH + "/deliveries/{deliveryId}", deliveryId)
                        .header(TENANT_HEADER, tenantId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(deliveryId));
    }

    @Test
    @Order(6)
    @DisplayName("Should return 200 when updating delivery status")
    void shouldReturn200_whenUpdatingDeliveryStatus() throws Exception {
        // Given
        assertThat(deliveryId).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String updateBody = """
                {
                    "status": "DELIVERED"
                }
                """;

        // When / Then
        mockMvc.perform(patch(EMAIL_BASE_PATH + "/deliveries/{deliveryId}", deliveryId)
                        .header(TENANT_HEADER, tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(deliveryId));
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
