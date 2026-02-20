/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.config.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tier 3 component test for {@link MockDataController}.
 *
 * <p>Boots the full Spring context with {@link AutoConfigureMockMvc} against
 * a Testcontainers MariaDB instance. Exercises the HTTP endpoint
 * {@code POST /v1/infra/mock-data/generate/all} end-to-end: request
 * deserialization, security filter chain, orchestrator delegation,
 * database persistence, and response serialization.</p>
 *
 * <p>Uses {@link MockMvc} for HTTP-layer testing and native SQL count
 * queries via {@link EntityManager} to bypass Hibernate tenant and
 * soft-delete filters when verifying record counts.</p>
 *
 * @author ElatusDev
 * @since 1.0
 * @see MockDataController
 * @see AbstractIntegrationTest
 */
@AutoConfigureMockMvc
@DisplayName("MockDataController — Component Test")
class MockDataControllerComponentTest extends AbstractIntegrationTest {

    private static final String ENDPOINT_PATH = "/v1/infra/mock-data/generate/all";
    private static final int ENTITIES_PER_TENANT = 5;
    private static final int TENANT_COUNT = 1;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("POST /v1/infra/mock-data/generate/all")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class GenerateAll {

        @Test
        @Order(1)
        @DisplayName("Should return 201 and populate all entity tables when given valid count")
        void shouldReturn201AndPopulateAllEntityTables_whenGivenValidCount() throws Exception {
            // Given — empty database from Testcontainers init script

            // When & Then — verify HTTP response
            mockMvc.perform(post(ENDPOINT_PATH)
                            .param("count", String.valueOf(ENTITIES_PER_TENANT)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Mock data generated")));

            // Then — verify database state via native SQL (bypasses Hibernate filters)

            // User tables
            assertThat(nativeCount("tenants"))
                    .as("tenants table should contain exactly %d rows", TENANT_COUNT)
                    .isEqualTo(TENANT_COUNT);
            assertThat(nativeCount("employees"))
                    .as("employees table should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("collaborators"))
                    .as("collaborators table should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("adult_students"))
                    .as("adult_students table should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("tutors"))
                    .as("tutors table should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("minor_students"))
                    .as("minor_students table should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);

            // Tenant-management tables
            assertThat(nativeCount("tenant_subscriptions"))
                    .as("tenant_subscriptions should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("tenant_billing_cycles"))
                    .as("tenant_billing_cycles should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);

            // Billing tables
            assertThat(nativeCount("compensations"))
                    .as("compensations should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("memberships"))
                    .as("memberships should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("membership_adult_students"))
                    .as("membership_adult_students should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("membership_tutors"))
                    .as("membership_tutors should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("payment_adult_students"))
                    .as("payment_adult_students should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("payment_tutors"))
                    .as("payment_tutors should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("card_payment_infos"))
                    .as("card_payment_infos should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);

            // Course-management tables
            assertThat(nativeCount("courses"))
                    .as("courses should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("schedules"))
                    .as("schedules should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("course_events"))
                    .as("course_events should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);

            // POS tables
            assertThat(nativeCount("store_products"))
                    .as("store_products should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("store_transactions"))
                    .as("store_transactions should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("store_sale_items"))
                    .as("store_sale_items should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);

            // Notification tables
            assertThat(nativeCount("notifications"))
                    .as("notifications should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("notification_deliveries"))
                    .as("notification_deliveries should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);

            // Email tables
            assertThat(nativeCount("emails"))
                    .as("emails should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("email_recipients"))
                    .as("email_recipients should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
            assertThat(nativeCount("email_attachments"))
                    .as("email_attachments should contain exactly %d rows", ENTITIES_PER_TENANT)
                    .isEqualTo(ENTITIES_PER_TENANT);
        }

        @Test
        @Order(2)
        @DisplayName("Should use default count of 50 when count parameter is omitted")
        void shouldUseDefaultCount_whenCountParameterIsOmitted() throws Exception {
            // Given — database already populated from previous test

            // When & Then — verify HTTP response
            mockMvc.perform(post(ENDPOINT_PATH))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("50")));

            // Then — verify database was repopulated with default count
            assertThat(nativeCount("tenants"))
                    .as("tenants should be exactly %d after default generation", TENANT_COUNT)
                    .isEqualTo(TENANT_COUNT);
            assertThat(nativeCount("employees"))
                    .as("employees should be exactly 50 after default generation")
                    .isEqualTo(50);
        }

        @Test
        @Order(3)
        @DisplayName("Should clean and reload without doubling records when called again")
        void shouldCleanAndReload_withoutDoublingRecords_whenCalledAgain() throws Exception {
            // Given — database populated with default 50 from previous test
            int reloadCount = 3;

            // When & Then — verify HTTP response
            mockMvc.perform(post(ENDPOINT_PATH)
                            .param("count", String.valueOf(reloadCount)))
                    .andExpect(status().isCreated());

            // Then — counts should be exactly reloadCount (not doubled)
            assertThat(nativeCount("tenants"))
                    .as("tenants count should be exactly %d after clean-and-reload", TENANT_COUNT)
                    .isEqualTo(TENANT_COUNT);
            assertThat(nativeCount("employees"))
                    .as("employees count should be exactly %d after clean-and-reload", reloadCount)
                    .isEqualTo(reloadCount);
            assertThat(nativeCount("courses"))
                    .as("courses count should be exactly %d after clean-and-reload", reloadCount)
                    .isEqualTo(reloadCount);
            assertThat(nativeCount("memberships"))
                    .as("memberships count should be exactly %d after clean-and-reload", reloadCount)
                    .isEqualTo(reloadCount);
            assertThat(nativeCount("notifications"))
                    .as("notifications count should be exactly %d after clean-and-reload", reloadCount)
                    .isEqualTo(reloadCount);
            assertThat(nativeCount("emails"))
                    .as("emails count should be exactly %d after clean-and-reload", reloadCount)
                    .isEqualTo(reloadCount);
            assertThat(nativeCount("store_sale_items"))
                    .as("store_sale_items count should be exactly %d after clean-and-reload", reloadCount)
                    .isEqualTo(reloadCount);
            assertThat(nativeCount("card_payment_infos"))
                    .as("card_payment_infos count should be exactly %d after clean-and-reload", reloadCount)
                    .isEqualTo(reloadCount);
        }
    }

    @Nested
    @DisplayName("Auxiliary tables")
    class AuxiliaryTables {

        @Test
        @DisplayName("Should populate auxiliary tables after successful generation")
        void shouldPopulateAuxiliaryTables_afterSuccessfulGeneration() throws Exception {
            // Given — ensure data is present
            mockMvc.perform(post(ENDPOINT_PATH)
                            .param("count", String.valueOf(ENTITIES_PER_TENANT)))
                    .andExpect(status().isCreated());

            // When — (data already loaded)

            // Then — verify supporting tables were populated
            assertThat(nativeCount("person_piis"))
                    .as("person_piis table should have records for all user types")
                    .isGreaterThan(0);
            assertThat(nativeCount("internal_auths"))
                    .as("internal_auths table should have records for employees and collaborators")
                    .isGreaterThan(0);
            assertThat(nativeCount("customer_auths"))
                    .as("customer_auths table should have records for customer types")
                    .isGreaterThan(0);
            assertThat(nativeCount("tenant_sequences"))
                    .as("tenant_sequences table should track ID sequences")
                    .isGreaterThan(0);
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
