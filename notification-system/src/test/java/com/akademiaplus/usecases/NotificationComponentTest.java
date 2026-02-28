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
 * Component test for {@code Notification} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the Notification entity.
 *
 * <p>Notification is a root entity with no foreign key dependencies.
 * Required fields: title, content, type, priority.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Notification — Component Test")
class NotificationComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/notification-system/notifications";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Notification Test Academy";
    private static final String TENANT_EMAIL = "admin@notificationtest.com";
    private static final String TENANT_ADDRESS = "300 Notification Ave";

    // ── Notification test data ────────────────────────────────────────
    private static final String NOTIFICATION1_TITLE = "System Maintenance";
    private static final String NOTIFICATION1_CONTENT = "Scheduled downtime tonight at 11 PM";
    private static final String NOTIFICATION1_TYPE = "ALERT";
    private static final String NOTIFICATION1_PRIORITY = "HIGH";

    private static final String NOTIFICATION2_TITLE = "New Feature Available";
    private static final String NOTIFICATION2_CONTENT = "Check out the new dashboard widgets";
    private static final String NOTIFICATION2_TYPE = "INFO";
    private static final String NOTIFICATION2_PRIORITY = "LOW";

    /**
     * Table names for {@code tenant_sequences} — Notification creation
     * requires a sequence for notifications.
     */
    private static final String[] ENTITY_TABLE_NAMES = {"notifications"};

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long notification1Id;
    private static Long notification2Id;

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
    @DisplayName("Should return 201 when creating notification with required fields")
    void shouldReturn201_whenCreatingNotificationWithRequiredFields() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "title": "%s",
                    "content": "%s",
                    "type": "%s",
                    "priority": "%s"
                }
                """.formatted(
                NOTIFICATION1_TITLE,
                NOTIFICATION1_CONTENT,
                NOTIFICATION1_TYPE,
                NOTIFICATION1_PRIORITY);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationId").isNumber())
                .andReturn();

        // Then
        notification1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.notificationId"))
                .longValue();
        assertThat(notification1Id).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 for second notification with unique data")
    void shouldReturn201_whenCreatingSecondNotification() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "title": "%s",
                    "content": "%s",
                    "type": "%s",
                    "priority": "%s"
                }
                """.formatted(
                NOTIFICATION2_TITLE,
                NOTIFICATION2_CONTENT,
                NOTIFICATION2_TYPE,
                NOTIFICATION2_PRIORITY);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationId").isNumber())
                .andReturn();

        // Then
        notification2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.notificationId"))
                .longValue();
        assertThat(notification2Id).isPositive();
        assertThat(notification2Id).isNotEqualTo(notification1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with notification details when found")
    void shouldReturn200_whenNotificationFound() throws Exception {
        // Given
        assertThat(notification1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", notification1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(notification1Id))
                .andExpect(jsonPath("$.title").value(NOTIFICATION1_TITLE))
                .andExpect(jsonPath("$.type").value(NOTIFICATION1_TYPE))
                .andExpect(jsonPath("$.priority").value(NOTIFICATION1_PRIORITY));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when notification not found")
    void shouldReturn404_whenNotificationNotFound() throws Exception {
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
    @DisplayName("Should return 200 with list when notifications exist")
    void shouldReturn200WithList_whenNotificationsExist() throws Exception {
        // Given
        assertThat(notification1Id).isNotNull();
        assertThat(notification2Id).isNotNull();
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
    @DisplayName("Should return 404 when deleting non-existent notification")
    void shouldReturn404_whenDeletingNonExistentNotification() throws Exception {
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
    @DisplayName("Should return 204 when deleting existing notification")
    void shouldReturn204_whenDeletingExistingNotification() throws Exception {
        // Given
        assertThat(notification2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", notification2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM notifications " +
                                "WHERE tenant_id = :tenantId AND notification_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", notification2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("Notification should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when requesting soft-deleted notification by ID")
    void shouldReturn404_whenRequestingSoftDeletedNotificationById() throws Exception {
        // Given — notification2Id was soft-deleted
        assertThat(notification2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", notification2Id)
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
