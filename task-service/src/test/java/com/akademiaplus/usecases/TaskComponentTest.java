/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.config.TaskControllerAdvice;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for {@code Task} CRUD operations.
 *
 * <p>Boots the full Spring context against a Testcontainers MariaDB instance
 * and verifies all REST endpoints for the Task entity.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Task — Component Test")
class TaskComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/tasks";

    // ── Tenant test data ──────────────────────────────────────────────
    private static final String TENANT_ORG_NAME = "Task Test Academy";
    private static final String TENANT_EMAIL = "admin@tasktest.com";
    private static final String TENANT_ADDRESS = "400 Task Blvd";

    // ── Task test data ────────────────────────────────────────────────
    private static final Long CREATED_BY_USER_ID = 42L;
    private static final Long ASSIGNEE_ID = 10L;
    private static final String ASSIGNEE_TYPE = "EMPLOYEE";

    private static final String TASK1_TITLE = "Prepare quarterly report";
    private static final String TASK1_DESCRIPTION = "Compile data for Q1 report";
    private static final String TASK1_PRIORITY = "HIGH";

    private static final String TASK2_TITLE = "Review student enrollments";
    private static final String TASK2_DESCRIPTION = "Verify enrollment data accuracy";
    private static final String TASK2_PRIORITY = "MEDIUM";

    private static final String[] ENTITY_TABLE_NAMES = {"tasks"};

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long task1Id;
    private static Long task2Id;

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
    @DisplayName("Should return 201 when creating task with required fields")
    void shouldReturn201_whenCreatingTaskWithRequiredFields() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String dueDate = LocalDate.now().plusDays(7).toString();
        String body = """
                {
                    "title": "%s",
                    "description": "%s",
                    "assigneeId": %d,
                    "assigneeType": "%s",
                    "dueDate": "%s",
                    "priority": "%s"
                }
                """.formatted(
                TASK1_TITLE, TASK1_DESCRIPTION,
                ASSIGNEE_ID, ASSIGNEE_TYPE,
                dueDate, TASK1_PRIORITY);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .with(authentication(createAuthToken(CREATED_BY_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value(TASK1_TITLE))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdBy").value(CREATED_BY_USER_ID))
                .andReturn();

        // Then
        task1Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.id"))
                .longValue();
        assertThat(task1Id).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should return 201 for second task with unique data")
    void shouldReturn201_whenCreatingSecondTask() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        String dueDate = LocalDate.now().plusDays(14).toString();
        String body = """
                {
                    "title": "%s",
                    "description": "%s",
                    "assigneeId": %d,
                    "assigneeType": "%s",
                    "dueDate": "%s",
                    "priority": "%s"
                }
                """.formatted(
                TASK2_TITLE, TASK2_DESCRIPTION,
                ASSIGNEE_ID, ASSIGNEE_TYPE,
                dueDate, TASK2_PRIORITY);

        // When
        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .with(authentication(createAuthToken(CREATED_BY_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        // Then
        task2Id = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.id"))
                .longValue();
        assertThat(task2Id).isPositive();
        assertThat(task2Id).isNotEqualTo(task1Id);
    }

    // ── GetById Tests ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with task details when found")
    void shouldReturn200_whenTaskFound() throws Exception {
        // Given
        assertThat(task1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", task1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task1Id))
                .andExpect(jsonPath("$.title").value(TASK1_TITLE))
                .andExpect(jsonPath("$.description").value(TASK1_DESCRIPTION))
                .andExpect(jsonPath("$.assigneeId").value(ASSIGNEE_ID))
                .andExpect(jsonPath("$.assigneeType").value(ASSIGNEE_TYPE))
                .andExpect(jsonPath("$.priority").value(TASK1_PRIORITY))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Order(4)
    @DisplayName("Should return 404 when task not found")
    void shouldReturn404_whenTaskNotFound() throws Exception {
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

    // ── List Tests ────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Should return 200 with task list when tasks exist")
    void shouldReturn200WithList_whenTasksExist() throws Exception {
        // Given
        assertThat(task1Id).isNotNull();
        assertThat(task2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalElements").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @Order(6)
    @DisplayName("Should return filtered tasks by priority")
    void shouldReturnFilteredTasks_whenPrioritySpecified() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH)
                        .param("priority", "HIGH")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ── Update Tests ──────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("Should return 200 when updating task title")
    void shouldReturn200_whenUpdatingTaskTitle() throws Exception {
        // Given
        assertThat(task1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);
        String updatedTitle = "Updated quarterly report";
        String body = """
                {
                    "title": "%s"
                }
                """.formatted(updatedTitle);

        // When / Then
        mockMvc.perform(put(BASE_PATH + "/{id}", task1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task1Id))
                .andExpect(jsonPath("$.title").value(updatedTitle))
                .andExpect(jsonPath("$.description").value(TASK1_DESCRIPTION));
    }

    @Test
    @Order(8)
    @DisplayName("Should return 404 when updating non-existent task")
    void shouldReturn404_whenUpdatingNonExistentTask() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        Long nonExistentId = 999999L;
        String body = """
                {
                    "title": "Does not exist"
                }
                """;

        // When / Then
        mockMvc.perform(put(BASE_PATH + "/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── Complete Tests ────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("Should return 200 with completedAt when marking task complete")
    void shouldReturn200_whenMarkingTaskComplete() throws Exception {
        // Given
        assertThat(task1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(patch(BASE_PATH + "/{id}/complete", task1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(task1Id))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @Order(10)
    @DisplayName("Should return 409 when completing already completed task")
    void shouldReturn409_whenCompletingAlreadyCompletedTask() throws Exception {
        // Given — task1Id was completed in Order(9)
        assertThat(task1Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(patch(BASE_PATH + "/{id}/complete", task1Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value(TaskControllerAdvice.CODE_TASK_ALREADY_COMPLETED));
    }

    @Test
    @Order(11)
    @DisplayName("Should return 404 when completing non-existent task")
    void shouldReturn404_whenCompletingNonExistentTask() throws Exception {
        // Given
        tenantContextHolder.setTenantId(tenantId);
        Long nonExistentId = 999999L;

        // When / Then
        mockMvc.perform(patch(BASE_PATH + "/{id}/complete", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── Delete Tests ──────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("Should return 404 when deleting non-existent task")
    void shouldReturn404_whenDeletingNonExistentTask() throws Exception {
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
    @Order(13)
    @DisplayName("Should return 204 when deleting existing task")
    void shouldReturn204_whenDeletingExistingTask() throws Exception {
        // Given
        assertThat(task2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When
        mockMvc.perform(delete(BASE_PATH + "/{id}", task2Id))
                .andExpect(status().isNoContent());

        // Then — verify soft-deleted via native SQL
        entityManager.clear();
        Object deletedAt = entityManager
                .createNativeQuery(
                        "SELECT deleted_at FROM tasks " +
                                "WHERE tenant_id = :tenantId AND task_id = :entityId")
                .setParameter("tenantId", tenantId)
                .setParameter("entityId", task2Id)
                .getSingleResult();
        assertThat(deletedAt)
                .as("Task should have deleted_at set after deletion")
                .isNotNull();
    }

    @Test
    @Order(14)
    @DisplayName("Should return 404 when requesting soft-deleted task by ID")
    void shouldReturn404_whenRequestingSoftDeletedTaskById() throws Exception {
        // Given — task2Id was soft-deleted
        assertThat(task2Id).isNotNull();
        tenantContextHolder.setTenantId(tenantId);

        // When / Then
        mockMvc.perform(get(BASE_PATH + "/{id}", task2Id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── Security Context Helper ───────────────────────────────────────

    private UsernamePasswordAuthenticationToken createAuthToken(Long userId) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        "test-user", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        token.setDetails(Map.of("userId", userId));
        return token;
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
