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
 * Component test for {@code StoreTransaction} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the StoreTransaction entity.
 *
 * <p>StoreTransaction is a root entity with no foreign key dependencies.
 * Required fields: transactionType, totalAmount, paymentMethod.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("StoreTransaction — Component Test")
class StoreTransactionComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/pos-system/store-transactions";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "StoreTransaction Test Academy";
    private static final String TENANT_EMAIL = "admin@storetxntest.com";
    private static final String TENANT_ADDRESS = "500 Transaction Way";

    // ── StoreTransaction test data ────────────────────────────────────
    private static final String TXN1_TYPE = "SALE";
    private static final double TXN1_AMOUNT = 49.99;
    private static final String TXN1_PAYMENT_METHOD = "CREDIT_CARD";

    private static final String TXN2_TYPE = "REFUND";
    private static final double TXN2_AMOUNT = 15.00;
    private static final String TXN2_PAYMENT_METHOD = "CASH";

    /**
     * Table names for {@code tenant_sequences} — StoreTransaction creation
     * requires a sequence for store_transactions.
     */
    private static final String[] ENTITY_TABLE_NAMES = {"store_transactions"};

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long txn1Id;
    private static Long txn2Id;

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
    @DisplayName("Should return 201 when creating store transaction with required fields")
    void shouldReturn201_whenCreatingStoreTransactionWithRequiredFields() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "transactionType": "%s",
                    "totalAmount": %s,
                    "paymentMethod": "%s"
                }
                """.formatted(TXN1_TYPE, TXN1_AMOUNT, TXN1_PAYMENT_METHOD);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeTransactionId").isNumber())
                .andReturn();

        // Then
        txn1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.storeTransactionId"))
                .longValue();
        assertThat(txn1Id).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 for second store transaction with unique data")
    void shouldReturn201_whenCreatingSecondStoreTransaction() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "transactionType": "%s",
                    "totalAmount": %s,
                    "paymentMethod": "%s"
                }
                """.formatted(TXN2_TYPE, TXN2_AMOUNT, TXN2_PAYMENT_METHOD);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeTransactionId").isNumber())
                .andReturn();

        // Then
        txn2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.storeTransactionId"))
                .longValue();
        assertThat(txn2Id).isPositive();
        assertThat(txn2Id).isNotEqualTo(txn1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with store transaction details when found")
    void shouldReturn200_whenStoreTransactionFound() throws Exception {
        // Given
        assertThat(txn1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", txn1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeTransactionId").value(txn1Id))
                .andExpect(jsonPath("$.transactionType").value(TXN1_TYPE))
                .andExpect(jsonPath("$.totalAmount").value(TXN1_AMOUNT))
                .andExpect(jsonPath("$.paymentMethod").value(TXN1_PAYMENT_METHOD));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when store transaction not found")
    void shouldReturn404_whenStoreTransactionNotFound() throws Exception {
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
    @DisplayName("Should return 200 with list when store transactions exist")
    void shouldReturn200WithList_whenStoreTransactionsExist() throws Exception {
        // Given
        assertThat(txn1Id).isNotNull();
        assertThat(txn2Id).isNotNull();
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
    @DisplayName("Should return 404 when deleting non-existent store transaction")
    void shouldReturn404_whenDeletingNonExistentStoreTransaction() throws Exception {
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
    @DisplayName("Should return 204 when deleting existing store transaction")
    void shouldReturn204_whenDeletingExistingStoreTransaction() throws Exception {
        // Given
        assertThat(txn2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", txn2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM store_transactions " +
                                "WHERE tenant_id = :tenantId AND store_transaction_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", txn2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("StoreTransaction should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when requesting soft-deleted store transaction by ID")
    void shouldReturn404_whenRequestingSoftDeletedStoreTransactionById() throws Exception {
        // Given — txn2Id was soft-deleted
        assertThat(txn2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", txn2Id)
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
