/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.tenancy.TenantDataModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tier 3 component test for tenant creation via the REST endpoint.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and exercises the {@code POST /v1/tenants} endpoint end-to-end. Verifies:
 * <ul>
 *   <li>Successful creation returns HTTP 201 with generated tenant ID</li>
 *   <li>Created tenant is persisted in the database with correct field values</li>
 * </ul>
 *
 * @author ElatusDev
 * @since 1.0
 * @see AbstractIntegrationTest
 */
@AutoConfigureMockMvc
@DisplayName("Tenant Creation — Component Test")
class TenantCreationComponentTest extends AbstractIntegrationTest {

    private static final String TENANTS_PATH = "/v1/tenants";

    private static final String TEST_ORG_NAME = "Test Academy";
    private static final String TEST_EMAIL = "admin@testacademy.com";
    private static final String TEST_ADDRESS = "456 Education Blvd";
    private static final String TEST_LEGAL_NAME = "Test Academy S.A. de C.V.";
    private static final String TEST_PHONE = "+525512345678";
    private static final String TEST_TAX_ID = "TAC2501011A1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Nested
    @DisplayName("Successful creation")
    class SuccessfulCreation {

        @Test
        @DisplayName("Should return 201 with tenant ID when given valid required fields")
        void shouldReturn201WithTenantId_whenGivenValidRequiredFields() throws Exception {
            // Given — a valid creation request with only required fields
            String requestBody = """
                    {
                        "organization_name": "%s",
                        "email": "%s",
                        "address": "%s"
                    }
                    """.formatted(TEST_ORG_NAME, TEST_EMAIL, TEST_ADDRESS);

            // When — POST to create tenant
            MvcResult result = mockMvc.perform(post(TENANTS_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tenant_id").isNumber())
                    .andExpect(jsonPath("$.organization_name").value(TEST_ORG_NAME))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.address").value(TEST_ADDRESS))
                    .andReturn();

            // Then — tenant exists in the database
            Long tenantId = ((Number) com.jayway.jsonpath.JsonPath
                    .read(result.getResponse().getContentAsString(), "$.tenant_id"))
                    .longValue();

            Optional<TenantDataModel> persisted = tenantRepository.findById(tenantId);
            assertThat(persisted)
                    .as("Tenant should be persisted in the database")
                    .isPresent();
            assertThat(persisted.get().getOrganizationName())
                    .isEqualTo(TEST_ORG_NAME);
            assertThat(persisted.get().getEmail())
                    .isEqualTo(TEST_EMAIL);
            assertThat(persisted.get().getAddress())
                    .isEqualTo(TEST_ADDRESS);
        }

        @Test
        @DisplayName("Should persist all optional fields when provided")
        void shouldPersistAllOptionalFields_whenProvided() throws Exception {
            // Given — a creation request with all fields
            String requestBody = """
                    {
                        "organization_name": "%s",
                        "email": "%s",
                        "address": "%s",
                        "legal_name": "%s",
                        "phone": "%s",
                        "tax_id": "%s"
                    }
                    """.formatted(TEST_ORG_NAME, "full@testacademy.com",
                    TEST_ADDRESS, TEST_LEGAL_NAME, TEST_PHONE, TEST_TAX_ID);

            // When — POST to create tenant
            MvcResult result = mockMvc.perform(post(TENANTS_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tenant_id").isNumber())
                    .andExpect(jsonPath("$.legal_name").value(TEST_LEGAL_NAME))
                    .andExpect(jsonPath("$.phone").value(TEST_PHONE))
                    .andExpect(jsonPath("$.tax_id").value(TEST_TAX_ID))
                    .andReturn();

            // Then — all fields are persisted
            Long tenantId = ((Number) com.jayway.jsonpath.JsonPath
                    .read(result.getResponse().getContentAsString(), "$.tenant_id"))
                    .longValue();

            TenantDataModel persisted = tenantRepository.findById(tenantId).orElseThrow();
            assertThat(persisted.getLegalName()).isEqualTo(TEST_LEGAL_NAME);
            assertThat(persisted.getPhone()).isEqualTo(TEST_PHONE);
            assertThat(persisted.getTaxId()).isEqualTo(TEST_TAX_ID);
        }
    }

}
