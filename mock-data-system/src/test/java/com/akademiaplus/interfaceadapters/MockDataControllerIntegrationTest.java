package com.akademiaplus.interfaceadapters;

import com.akademiaplus.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class MockDataControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String TEST_TENANT_ID = "1";
    private static final String GENERATE_ALL_PATH = "/v1/infra/mock-data/generate/all";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnCreated_whenGeneratingMockDataWithDefaultCount() throws Exception {
        mockMvc.perform(withTenant(post(GENERATE_ALL_PATH))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().string("Mock data generated: 50 records per entity type."));
    }

    @Test
    void shouldReturnCreated_whenGeneratingMockDataWithCustomCount() throws Exception {
        mockMvc.perform(withTenant(post(GENERATE_ALL_PATH + "?count=5"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().string("Mock data generated: 5 records per entity type."));
    }

    private MockHttpServletRequestBuilder withTenant(MockHttpServletRequestBuilder builder) {
        return builder.header("X-Tenant-Id", TEST_TENANT_ID);
    }
}
