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
 * Component test for {@code StoreCatalog} read-only operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies the read-only catalog REST endpoints.
 *
 * <p>The catalog endpoint serves store products that are active (not soft-deleted)
 * and have stock > 0. Products are created via the StoreProduct POST endpoint
 * and then queried through the catalog endpoints.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("StoreCatalog — Component Test")
class StoreCatalogComponentTest extends AbstractIntegrationTest {

    private static final String CATALOG_PATH = "/v1/pos-system/store/catalog";
    private static final String PRODUCTS_PATH = "/v1/pos-system/store-products";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Catalog Test Academy";
    private static final String TENANT_EMAIL = "admin@catalogtest.com";
    private static final String TENANT_ADDRESS = "700 Catalog Drive";

    // ── Product test data ─────────────────────────────────────────────
    private static final String PRODUCT1_NAME = "Calculus Textbook";
    private static final double PRODUCT1_PRICE = 45.99;
    private static final int PRODUCT1_STOCK = 20;
    private static final String PRODUCT1_CATEGORY = "Books";

    private static final String PRODUCT2_NAME = "Lab Coat";
    private static final double PRODUCT2_PRICE = 25.00;
    private static final int PRODUCT2_STOCK = 50;
    private static final String PRODUCT2_CATEGORY = "Apparel";

    /**
     * Table names for {@code tenant_sequences} — Store catalog uses
     * the store_products sequence.
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

    // ── Setup — Create Products ───────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Should create products for catalog testing")
    void shouldCreateProducts_forCatalogTesting() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body1 = """
                {
                    "name": "%s",
                    "price": %s,
                    "stockQuantity": %d,
                    "category": "%s"
                }
                """.formatted(PRODUCT1_NAME, PRODUCT1_PRICE, PRODUCT1_STOCK, PRODUCT1_CATEGORY);

        // When
        MvcResult result1 = mockMvc.perform(post(PRODUCTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isCreated())
                .andReturn();

        product1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result1.getResponse().getContentAsString(), "$.storeProductId"))
                .longValue();

        String body2 = """
                {
                    "name": "%s",
                    "price": %s,
                    "stockQuantity": %d,
                    "category": "%s"
                }
                """.formatted(PRODUCT2_NAME, PRODUCT2_PRICE, PRODUCT2_STOCK, PRODUCT2_CATEGORY);

        MvcResult result2 = mockMvc.perform(post(PRODUCTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isCreated())
                .andReturn();

        product2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result2.getResponse().getContentAsString(), "$.storeProductId"))
                .longValue();

        // Then
        assertThat(product1Id).isPositive();
        assertThat(product2Id).isPositive();
    }

    // ── Catalog GetAll Tests ──────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Should return 200 with catalog items when products exist")
    void shouldReturn200WithCatalogItems_whenProductsExist() throws Exception {
        // Given
        assertThat(product1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(CATALOG_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @Order(3)
    @DisplayName("Should filter catalog by category when category parameter provided")
    void shouldFilterByCategory_whenCategoryProvided() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(CATALOG_PATH)
                        .param("category", PRODUCT1_CATEGORY)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].category").value(PRODUCT1_CATEGORY));
    }

    // ── Catalog GetById Tests ─────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Should return 200 with catalog item details and inStock flag")
    void shouldReturn200WithInStockFlag_whenCatalogItemFound() throws Exception {
        // Given
        assertThat(product1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(CATALOG_PATH + "/{id}", product1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeProductId").value(product1Id))
                .andExpect(jsonPath("$.name").value(PRODUCT1_NAME))
                .andExpect(jsonPath("$.price").value(PRODUCT1_PRICE))
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.category").value(PRODUCT1_CATEGORY));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 when catalog item not found")
    void shouldReturn404_whenCatalogItemNotFound() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        Long nonExistentId = 999999L;

        // When / Then
        mockMvc.perform(get(CATALOG_PATH + "/{id}", nonExistentId)
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
