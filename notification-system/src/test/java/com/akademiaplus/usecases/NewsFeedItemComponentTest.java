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
 * Component test for {@code NewsFeedItem} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the NewsFeedItem entity.
 *
 * <p>NewsFeedItem is a root entity with no foreign key dependencies.
 * Required fields: title, body, authorId.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("NewsFeedItem — Component Test")
class NewsFeedItemComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/notification-system/news-feed";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "NewsFeed Test Academy";
    private static final String TENANT_EMAIL = "admin@newsfeedtest.com";
    private static final String TENANT_ADDRESS = "500 News Feed Blvd";

    // ── NewsFeedItem test data ────────────────────────────────────────
    private static final String ITEM1_TITLE = "Welcome to the Academy";
    private static final String ITEM1_BODY = "We are excited to announce the new academic year.";
    private static final Long ITEM1_AUTHOR_ID = 1L;

    private static final String ITEM2_TITLE = "Exam Schedule Published";
    private static final String ITEM2_BODY = "Final exams will begin on March 20th.";
    private static final Long ITEM2_AUTHOR_ID = 2L;

    /**
     * Table names for {@code tenant_sequences} — NewsFeedItem creation
     * requires a sequence for news_feed_items.
     */
    private static final String[] ENTITY_TABLE_NAMES = {"news_feed_items"};

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long item1Id;
    private static Long item2Id;

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
    @DisplayName("Should return 201 when creating news feed item as DRAFT by default")
    void shouldReturn201_whenCreatingNewsFeedItemAsDraft() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "title": "%s",
                    "body": "%s",
                    "authorId": %d
                }
                """.formatted(ITEM1_TITLE, ITEM1_BODY, ITEM1_AUTHOR_ID);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newsFeedItemId").isNumber())
                .andReturn();

        // Then
        item1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.newsFeedItemId"))
                .longValue();
        assertThat(item1Id).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 when creating published news feed item")
    void shouldReturn201_whenCreatingPublishedNewsFeedItem() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "title": "%s",
                    "body": "%s",
                    "authorId": %d,
                    "status": "PUBLISHED"
                }
                """.formatted(ITEM2_TITLE, ITEM2_BODY, ITEM2_AUTHOR_ID);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newsFeedItemId").isNumber())
                .andReturn();

        // Then
        item2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.newsFeedItemId"))
                .longValue();
        assertThat(item2Id).isPositive();
        assertThat(item2Id).isNotEqualTo(item1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with news feed item details when found")
    void shouldReturn200_whenNewsFeedItemFound() throws Exception {
        // Given
        assertThat(item1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", item1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsFeedItemId").value(item1Id))
                .andExpect(jsonPath("$.title").value(ITEM1_TITLE))
                .andExpect(jsonPath("$.body").value(ITEM1_BODY))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when news feed item not found")
    void shouldReturn404_whenNewsFeedItemNotFound() throws Exception {
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
    @DisplayName("Should return 200 with list of published items")
    void shouldReturn200WithList_whenPublishedItemsExist() throws Exception {
        // Given — item2 was created as PUBLISHED
        assertThat(item2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then — only published items should appear
        mockMvc.perform(get(BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ── Update Tests ──────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("Should return 200 when updating news feed item status to PUBLISHED")
    void shouldReturn200_whenUpdatingStatusToPublished() throws Exception {
        // Given
        assertThat(item1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "title": "%s",
                    "body": "%s",
                    "authorId": %d,
                    "status": "PUBLISHED"
                }
                """.formatted(ITEM1_TITLE, ITEM1_BODY, ITEM1_AUTHOR_ID);

        // When
        mockMvc.perform(put(BASE_PATH + "/{id}", item1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsFeedItemId").value(item1Id));

        // Then — verify publishedAt was set
        mockMvc.perform(get(BASE_PATH + "/{id}", item1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("Should return 404 when updating non-existent news feed item")
    void shouldReturn404_whenUpdatingNonExistentNewsFeedItem() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        Long nonExistentId = 999999L;
        String body = """
                {
                    "title": "Non-existent",
                    "body": "Does not exist",
                    "authorId": 1
                }
                """;

        // When / Then
        mockMvc.perform(put(BASE_PATH + "/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── Delete Tests ──────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("Should return 404 when deleting non-existent news feed item")
    void shouldReturn404_whenDeletingNonExistentNewsFeedItem() throws Exception {
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
    @Order(9)
    @DisplayName("Should return 204 when deleting existing news feed item")
    void shouldReturn204_whenDeletingExistingNewsFeedItem() throws Exception {
        // Given
        assertThat(item2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", item2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM news_feed_items " +
                                "WHERE tenant_id = :tenantId AND news_feed_item_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", item2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("NewsFeedItem should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(10)
    @DisplayName("Should return 404 when requesting soft-deleted news feed item by ID")
    void shouldReturn404_whenRequestingSoftDeletedNewsFeedItemById() throws Exception {
        // Given — item2Id was soft-deleted
        assertThat(item2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", item2Id)
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
