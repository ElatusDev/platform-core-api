/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import com.akademiaplus.leadmanagement.interfaceadapters.DemoRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component tests for demo request CRUD operations.
 *
 * <p>Full Spring context with Testcontainers MariaDB. Demo requests are
 * platform-level entities (not tenant-scoped), so no tenant setup is needed.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DemoRequestComponentTest")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoRequestComponentTest extends AbstractIntegrationTest {

    public static final String BASE_PATH = "/v1/lead-management/demo-requests";
    public static final String FIRST_NAME = "Carlos";
    public static final String LAST_NAME = "Gutierrez";
    public static final String EMAIL = "carlos.gutierrez@example.com";
    public static final String COMPANY_NAME = "Escuela Nacional";
    public static final String MESSAGE = "Interested in the platform for our school";
    public static final String DUPLICATE_EMAIL = "duplicate@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private DemoRequestRepository demoRequestRepository;

    private static Long createdId;

    @BeforeEach
    void cleanUp() {
        if (createdId == null) {
            demoRequestRepository.deleteAll();
        }
    }

    @Nested
    @Order(1)
    @DisplayName("Create Demo Request")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Create {

        @Test
        @Order(1)
        @DisplayName("Should return 201 when creating a valid demo request")
        void shouldReturn201_whenCreatingValidDemoRequest() throws Exception {
            // Given
            String body = """
                    {
                        "first_name": "%s",
                        "last_name": "%s",
                        "email": "%s",
                        "company_name": "%s",
                        "message": "%s"
                    }
                    """.formatted(FIRST_NAME, LAST_NAME, EMAIL, COMPANY_NAME, MESSAGE);

            // When
            MvcResult result = mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.demo_request_id").isNumber())
                    .andReturn();

            // Then
            createdId = ((Number) com.jayway.jsonpath.JsonPath
                    .read(result.getResponse().getContentAsString(), "$.demo_request_id"))
                    .longValue();
            assertThat(createdId).isPositive();
        }

        @Test
        @Order(2)
        @DisplayName("Should return 409 when creating a demo request with duplicate email")
        void shouldReturn409_whenDuplicateEmail() throws Exception {
            // Given — a demo request with EMAIL already exists from previous test
            String body = """
                    {
                        "first_name": "Another",
                        "last_name": "Person",
                        "email": "%s",
                        "company_name": "Other Company"
                    }
                    """.formatted(EMAIL);

            // When / Then
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE_ENTITY"));
        }

        @Test
        @Order(3)
        @DisplayName("Should return 400 when required fields are missing")
        void shouldReturn400_whenRequiredFieldsMissing() throws Exception {
            // Given — empty body (missing required firstName, lastName, email, companyName)
            String body = """
                    {
                        "message": "Only optional field"
                    }
                    """;

            // When / Then
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @Order(2)
    @DisplayName("Get Demo Request")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class GetById {

        @Test
        @Order(4)
        @DisplayName("Should return 200 when getting an existing demo request by ID")
        void shouldReturn200_whenGettingExistingDemoRequest() throws Exception {
            // Given — createdId set by the Create test
            assertThat(createdId).isNotNull();

            // When / Then
            mockMvc.perform(get(BASE_PATH + "/{id}", createdId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.demo_request_id").value(createdId))
                    .andExpect(jsonPath("$.first_name").value(FIRST_NAME))
                    .andExpect(jsonPath("$.last_name").value(LAST_NAME))
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.company_name").value(COMPANY_NAME))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @Order(5)
        @DisplayName("Should return 404 when getting a non-existent demo request")
        void shouldReturn404_whenDemoRequestNotFound() throws Exception {
            // Given
            long nonExistentId = 999999L;

            // When / Then
            mockMvc.perform(get(BASE_PATH + "/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ENTITY_NOT_FOUND"));
        }
    }

    @Nested
    @Order(3)
    @DisplayName("Get All Demo Requests")
    class GetAll {

        @Test
        @Order(6)
        @DisplayName("Should return 200 with demo requests list")
        void shouldReturn200_withDemoRequestsList() throws Exception {
            // Given — at least one demo request exists from Create test

            // When / Then
            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.demo_requests").isArray())
                    .andExpect(jsonPath("$.demo_requests.length()").value(
                            org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        }
    }

    @Nested
    @Order(4)
    @DisplayName("Delete Demo Request")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Delete {

        @Test
        @Order(7)
        @DisplayName("Should return 204 when deleting an existing demo request")
        void shouldReturn204_whenDeletingExistingDemoRequest() throws Exception {
            // Given — createdId set by the Create test
            assertThat(createdId).isNotNull();

            // When / Then
            mockMvc.perform(delete(BASE_PATH + "/{id}", createdId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @Order(8)
        @DisplayName("Should return 404 when deleting a non-existent demo request")
        void shouldReturn404_whenDeletingNonExistentDemoRequest() throws Exception {
            // Given
            long nonExistentId = 999999L;

            // When / Then
            mockMvc.perform(delete(BASE_PATH + "/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ENTITY_NOT_FOUND"));
        }
    }
}
