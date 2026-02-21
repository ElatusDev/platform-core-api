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
 * Component test for {@code StoreProduct} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the StoreProduct entity.
 *
 * <p>StoreProduct is a root entity with no foreign key dependencies.
 * Required fields: name, price, stockQuantity.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("StoreProduct — Component Test")
class StoreProductComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/pos-system/store-products";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "StoreProduct Test Academy";
    private static final String TENANT_EMAIL = "admin@storeproducttest.com";
    private static final String TENANT_ADDRESS = "400 Product Blvd";

    // ── StoreProduct test data ────────────────────────────────────────
    private static final String PRODUCT1_NAME = "Notebook A5";
    private static final String PRODUCT1_DESCRIPTION = "Premium quality A5 notebook";
    private static final double PRODUCT1_PRICE = 12.99;
    private static final int PRODUCT1_STOCK = 150;

    private static final String PRODUCT2_NAME = "Ballpoint Pen Set";
    private static final String PRODUCT2_DESCRIPTION = "Set of 10 ballpoint pens";
    private static final double PRODUCT2_PRICE = 8.50;
    private static final int PRODUCT2_STOCK = 300;

    /**
     * Table names for {@code tenant_sequences} — StoreProduct creation
     * requires a sequence for store_products.
     */
    private static final String[] ENTITY_TABLE_NAMES = {"store_products"};

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long product1Id;
    private static Long product2Id;

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
    @DisplayName("Should return 201 when creating store product with required fields")
    void shouldReturn201_whenCreatingStoreProductWithRequiredFields() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "name": "%s",
                    "description": "%s",
                    "price": %s,
                    "stockQuantity": %d
                }
                """.formatted(
                PRODUCT1_NAME,
                PRODUCT1_DESCRIPTION,
                PRODUCT1_PRICE,
                PRODUCT1_STOCK);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeProductId").isNumber())
                .andReturn();

        // Then
        product1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.storeProductId"))
                .longValue();
        assertThat(product1Id).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 for second store product with unique data")
    void shouldReturn201_whenCreatingSecondStoreProduct() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "name": "%s",
                    "description": "%s",
                    "price": %s,
                    "stockQuantity": %d
                }
                """.formatted(
                PRODUCT2_NAME,
                PRODUCT2_DESCRIPTION,
                PRODUCT2_PRICE,
                PRODUCT2_STOCK);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeProductId").isNumber())
                .andReturn();

        // Then
        product2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.storeProductId"))
                .longValue();
        assertThat(product2Id).isPositive();
        assertThat(product2Id).isNotEqualTo(product1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with store product details when found")
    void shouldReturn200_whenStoreProductFound() throws Exception {
        // Given
        assertThat(product1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", product1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeProductId").value(product1Id))
                .andExpect(jsonPath("$.name").value(PRODUCT1_NAME))
                .andExpect(jsonPath("$.price").value(PRODUCT1_PRICE))
                .andExpect(jsonPath("$.stockQuantity").value(PRODUCT1_STOCK));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when store product not found")
    void shouldReturn404_whenStoreProductNotFound() throws Exception {
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
    @DisplayName("Should return 200 with list when store products exist")
    void shouldReturn200WithList_whenStoreProductsExist() throws Exception {
        // Given
        assertThat(product1Id).isNotNull();
        assertThat(product2Id).isNotNull();
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
    @DisplayName("Should return 404 when deleting non-existent store product")
    void shouldReturn404_whenDeletingNonExistentStoreProduct() throws Exception {
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
    @DisplayName("Should return 204 when deleting existing store product")
    void shouldReturn204_whenDeletingExistingStoreProduct() throws Exception {
        // Given
        assertThat(product2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", product2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM store_products " +
                                "WHERE tenant_id = :tenantId AND store_product_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", product2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("StoreProduct should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when requesting soft-deleted store product by ID")
    void shouldReturn404_whenRequestingSoftDeletedStoreProductById() throws Exception {
        // Given — product2Id was soft-deleted
        assertThat(product2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", product2Id)
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
