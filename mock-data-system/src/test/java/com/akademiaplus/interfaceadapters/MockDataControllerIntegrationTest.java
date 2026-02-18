/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.config.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link MockDataController}.
 *
 * <p>Verifies the {@code /v1/infra/mock-data/generate/all} endpoint against
 * a real MariaDB instance managed by Testcontainers. Each test runs inside
 * a transaction that is rolled back automatically.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("MockDataController integration tests")
class MockDataControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TEST_TENANT_ID = "1";
    private static final String GENERATE_ALL_PATH = "/v1/infra/mock-data/generate/all";

    private static final int DEFAULT_COUNT = 50;
    private static final int CUSTOM_COUNT = 5;
    private static final String RESPONSE_TEMPLATE = "Mock data generated: %d records per entity type.";

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Generate all mock data")
    class GenerateAllMockData {

        @Test
        @DisplayName("Should return created with default count when no count parameter provided")
        void shouldReturnCreatedWithDefaultCount_whenNoCountParameterProvided() throws Exception {
            // Given
            String expectedResponse = String.format(RESPONSE_TEMPLATE, DEFAULT_COUNT);

            // When & Then
            mockMvc.perform(withTenant(post(GENERATE_ALL_PATH))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(expectedResponse));
        }

        @Test
        @DisplayName("Should return created with custom count when count parameter provided")
        void shouldReturnCreatedWithCustomCount_whenCountParameterProvided() throws Exception {
            // Given
            String expectedResponse = String.format(RESPONSE_TEMPLATE, CUSTOM_COUNT);

            // When & Then
            mockMvc.perform(withTenant(post(GENERATE_ALL_PATH + "?count=" + CUSTOM_COUNT))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(expectedResponse));
        }
    }

    private MockHttpServletRequestBuilder withTenant(MockHttpServletRequestBuilder builder) {
        return builder.header(TENANT_HEADER, TEST_TENANT_ID);
    }
}
