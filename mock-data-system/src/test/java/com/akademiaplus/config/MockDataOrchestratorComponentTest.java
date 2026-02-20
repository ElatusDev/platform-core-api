/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier 3 component test for {@link MockDataOrchestrator}.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB
 * instance and exercises the complete mock-data generation pipeline:
 * FK-safe DAG ordering, transaction propagation, EntityIdAssigner,
 * encrypted-field round-trips, and multi-entity cleanup.</p>
 *
 * <p>Uses native SQL count queries via {@link EntityManager} to bypass
 * Hibernate tenant and soft-delete filters when verifying record counts.</p>
 *
 * @author ElatusDev
 * @since 1.0
 * @see MockDataOrchestrator
 * @see AbstractIntegrationTest
 */
@DisplayName("MockDataOrchestrator — Component Test")
class MockDataOrchestratorComponentTest extends AbstractIntegrationTest {

    private static final int TENANT_COUNT = 1;
    private static final int ENTITIES_PER_TENANT = 5;

    @Autowired
    private MockDataOrchestrator orchestrator;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("generateAll(tenantCount, entitiesPerTenant)")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class GenerateAll {

        @Test
        @Order(1)
        @DisplayName("shouldPopulateAllEntityTables_whenGivenValidCount")
        void shouldPopulateAllEntityTables_whenGivenValidCount() {
            // Given — empty database from Testcontainers init script

            // When
            assertDoesNotThrow(() -> orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT));

            // Then — verify records exist in each entity table
            assertEquals(TENANT_COUNT, nativeCount("tenants"),
                    "tenants table should contain exactly " + TENANT_COUNT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("employees"),
                    "employees table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("collaborators"),
                    "collaborators table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("adult_students"),
                    "adult_students table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("tutors"),
                    "tutors table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("minor_students"),
                    "minor_students table should contain exactly " + ENTITIES_PER_TENANT + " rows");

            // Tenant-management tables
            assertEquals(ENTITIES_PER_TENANT, nativeCount("tenant_subscriptions"),
                    "tenant_subscriptions table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("tenant_billing_cycles"),
                    "tenant_billing_cycles table should contain exactly " + ENTITIES_PER_TENANT + " rows");

            // Billing tables
            assertEquals(ENTITIES_PER_TENANT, nativeCount("compensations"),
                    "compensations table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("memberships"),
                    "memberships table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("membership_adult_students"),
                    "membership_adult_students table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("membership_tutors"),
                    "membership_tutors table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("payment_adult_students"),
                    "payment_adult_students table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("payment_tutors"),
                    "payment_tutors table should contain exactly " + ENTITIES_PER_TENANT + " rows");

            // Course-management tables
            assertEquals(ENTITIES_PER_TENANT, nativeCount("courses"),
                    "courses table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("schedules"),
                    "schedules table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("course_events"),
                    "course_events table should contain exactly " + ENTITIES_PER_TENANT + " rows");

            // POS tables
            assertEquals(ENTITIES_PER_TENANT, nativeCount("store_products"),
                    "store_products table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("store_transactions"),
                    "store_transactions table should contain exactly " + ENTITIES_PER_TENANT + " rows");

            // Notification tables
            assertEquals(ENTITIES_PER_TENANT, nativeCount("notifications"),
                    "notifications table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("notification_deliveries"),
                    "notification_deliveries table should contain exactly " + ENTITIES_PER_TENANT + " rows");

            // Card payment info table
            assertEquals(ENTITIES_PER_TENANT, nativeCount("card_payment_infos"),
                    "card_payment_infos table should contain exactly " + ENTITIES_PER_TENANT + " rows");

            // Store sale items table
            assertEquals(ENTITIES_PER_TENANT, nativeCount("store_sale_items"),
                    "store_sale_items table should contain exactly " + ENTITIES_PER_TENANT + " rows");

            // Email tables
            assertEquals(ENTITIES_PER_TENANT, nativeCount("emails"),
                    "emails table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("email_recipients"),
                    "email_recipients table should contain exactly " + ENTITIES_PER_TENANT + " rows");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("email_attachments"),
                    "email_attachments table should contain exactly " + ENTITIES_PER_TENANT + " rows");
        }

        @Test
        @Order(2)
        @DisplayName("shouldPopulateAuxiliaryTables_whenEntitiesAreCreated")
        void shouldPopulateAuxiliaryTables_whenEntitiesAreCreated() {
            // Given — data from the previous test run (ordered execution)

            // When — (data already loaded by previous test)

            // Then — verify supporting tables were populated
            assertTrue(nativeCount("person_piis") > 0,
                    "person_piis table should have records for all user types");
            assertTrue(nativeCount("internal_auths") > 0,
                    "internal_auths table should have records for employees and collaborators");
            assertTrue(nativeCount("customer_auths") > 0,
                    "customer_auths table should have records for customer types");
            assertTrue(nativeCount("tenant_sequences") > 0,
                    "tenant_sequences table should track ID sequences");
        }

        @Test
        @Order(3)
        @DisplayName("shouldCleanAndReload_whenCalledAgain")
        void shouldCleanAndReload_whenCalledAgain() {
            // Given — database already populated from previous tests

            // When — calling generateAll again triggers cleanup + fresh load
            assertDoesNotThrow(() -> orchestrator.generateAll(TENANT_COUNT, ENTITIES_PER_TENANT));

            // Then — counts should remain exactly ENTITIES_PER_TENANT (not doubled)
            assertEquals(TENANT_COUNT, nativeCount("tenants"),
                    "tenants count should be exactly " + TENANT_COUNT + " after clean-and-reload");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("employees"),
                    "employees count should be exactly " + ENTITIES_PER_TENANT + " after clean-and-reload");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("minor_students"),
                    "minor_students count should be exactly " + ENTITIES_PER_TENANT + " after clean-and-reload");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("courses"),
                    "courses count should be exactly " + ENTITIES_PER_TENANT + " after clean-and-reload");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("memberships"),
                    "memberships count should be exactly " + ENTITIES_PER_TENANT + " after clean-and-reload");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("notifications"),
                    "notifications count should be exactly " + ENTITIES_PER_TENANT + " after clean-and-reload");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("card_payment_infos"),
                    "card_payment_infos count should be exactly " + ENTITIES_PER_TENANT + " after clean-and-reload");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("store_sale_items"),
                    "store_sale_items count should be exactly " + ENTITIES_PER_TENANT + " after clean-and-reload");
            assertEquals(ENTITIES_PER_TENANT, nativeCount("emails"),
                    "emails count should be exactly " + ENTITIES_PER_TENANT + " after clean-and-reload");
        }
    }

    /**
     * Executes a native SQL count query, bypassing all Hibernate filters
     * (tenant filter, soft-delete filter).
     *
     * @param tableName the database table to count
     * @return the number of rows in the table
     */
    private long nativeCount(String tableName) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + tableName)
                .getSingleResult())
                .longValue();
    }
}
