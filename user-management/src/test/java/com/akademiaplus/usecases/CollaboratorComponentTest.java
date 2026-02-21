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
 * Component test for {@code Collaborator} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the Collaborator entity.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Collaborator — Component Test")
class CollaboratorComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/user-management/collaborators";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Collaborator Test Academy";
    private static final String TENANT_EMAIL = "admin@collabtest.com";
    private static final String TENANT_ADDRESS = "200 Collaborator Blvd";

    // ── Collaborator test data ────────────────────────────────────────
    private static final String COLLAB_SKILLS = "Mathematics, Physics";
    private static final String COLLAB_BIRTHDATE = "1988-03-20";
    private static final String COLLAB_ENTRY_DATE = "2021-01-15";
    private static final String COLLAB_ROLE = "INSTRUCTOR";
    private static final String COMMON_ADDRESS = "456 Test Ave";
    private static final String COMMON_ZIP_CODE = "10000";

    private static final String COLLAB1_FIRST_NAME = "CollabOneFirst";
    private static final String COLLAB1_LAST_NAME = "CollabOneLast";
    private static final String COLLAB1_EMAIL = "collab1@collabtest.com";
    private static final String COLLAB1_PHONE = "+525599990011";
    private static final String COLLAB1_USERNAME = "collabuser1";
    private static final String COLLAB1_PASSWORD = "CollabPass1!";

    private static final String COLLAB2_FIRST_NAME = "CollabTwoFirst";
    private static final String COLLAB2_LAST_NAME = "CollabTwoLast";
    private static final String COLLAB2_EMAIL = "collab2@collabtest.com";
    private static final String COLLAB2_PHONE = "+525599990012";
    private static final String COLLAB2_USERNAME = "collabuser2";
    private static final String COLLAB2_PASSWORD = "CollabPass2!";

    /**
     * Table names for {@code tenant_sequences} — Collaborator creation
     * requires sequences for collaborators, person_piis, and internal_auths.
     */
    private static final String[] ENTITY_TABLE_NAMES = {
            "collaborators", "person_piis", "internal_auths"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long collaborator1Id;
    private static Long collaborator2Id;

    @BeforeEach
    void setUpTestDataOnce() throws Exception {
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
    @DisplayName("Should return 201 with all fields when given complete request")
    void shouldReturn201WithAllFields_whenGivenCompleteRequest() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "skills": "%s",
                    "birthdate": "%s",
                    "entryDate": "%s",
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s",
                    "phoneNumber": "%s",
                    "address": "%s",
                    "zipCode": "%s",
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                }
                """.formatted(COLLAB_SKILLS, COLLAB_BIRTHDATE, COLLAB_ENTRY_DATE,
                COLLAB1_FIRST_NAME, COLLAB1_LAST_NAME, COLLAB1_EMAIL, COLLAB1_PHONE,
                COMMON_ADDRESS, COMMON_ZIP_CODE, COLLAB1_USERNAME, COLLAB1_PASSWORD, COLLAB_ROLE);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.collaboratorId").isNumber())
                .andReturn();

        // Then
        collaborator1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.collaboratorId"))
                .longValue();
        assertThat(collaborator1Id).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 for second collaborator with unique credentials")
    void shouldReturn201_whenCreatingSecondCollaborator() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String body = """
                {
                    "skills": "%s",
                    "birthdate": "%s",
                    "entryDate": "%s",
                    "firstName": "%s",
                    "lastName": "%s",
                    "email": "%s",
                    "phoneNumber": "%s",
                    "address": "%s",
                    "zipCode": "%s",
                    "username": "%s",
                    "password": "%s",
                    "role": "%s"
                }
                """.formatted(COLLAB_SKILLS, COLLAB_BIRTHDATE, COLLAB_ENTRY_DATE,
                COLLAB2_FIRST_NAME, COLLAB2_LAST_NAME, COLLAB2_EMAIL, COLLAB2_PHONE,
                COMMON_ADDRESS, COMMON_ZIP_CODE, COLLAB2_USERNAME, COLLAB2_PASSWORD, COLLAB_ROLE);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.collaboratorId").isNumber())
                .andReturn();

        // Then
        collaborator2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.collaboratorId"))
                .longValue();
        assertThat(collaborator2Id).isPositive();
        assertThat(collaborator2Id).isNotEqualTo(collaborator1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with collaborator details when found")
    void shouldReturn200_whenCollaboratorFound() throws Exception {
        // Given
        assertThat(collaborator1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", collaborator1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collaboratorId").value(collaborator1Id))
                .andExpect(jsonPath("$.firstName").value(COLLAB1_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(COLLAB1_LAST_NAME))
                .andExpect(jsonPath("$.skills").value(COLLAB_SKILLS));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when collaborator not found")
    void shouldReturn404_whenCollaboratorNotFound() throws Exception {
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
    @DisplayName("Should return 200 with list when collaborators exist")
    void shouldReturn200WithList_whenCollaboratorsExist() throws Exception {
        // Given
        assertThat(collaborator1Id).isNotNull();
        assertThat(collaborator2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    // ── Delete Tests ──────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("Should return 404 when deleting non-existent collaborator")
    void shouldReturn404_whenDeletingNonExistentCollaborator() throws Exception {
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
    @DisplayName("Should return 204 when deleting existing collaborator")
    void shouldReturn204_whenDeletingExistingCollaborator() throws Exception {
        // Given
        assertThat(collaborator2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", collaborator2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM collaborators " +
                                "WHERE tenant_id = :tenantId AND collaborator_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", collaborator2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("Collaborator should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when requesting soft-deleted collaborator by ID")
    void shouldReturn404_whenRequestingSoftDeletedCollaboratorById() throws Exception {
        // Given — collaborator2Id was soft-deleted
        assertThat(collaborator2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", collaborator2Id)
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
