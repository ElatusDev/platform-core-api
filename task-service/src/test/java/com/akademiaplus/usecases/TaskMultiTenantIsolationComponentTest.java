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
 * Multi-tenant isolation component test for Tasks.
 *
 * <p>Creates a task in tenant A, then switches to tenant B and verifies
 * that tenant B cannot see, modify, complete, or delete tenant A's task.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Task — Multi-Tenant Isolation Component Test")
class TaskMultiTenantIsolationComponentTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/tasks";
    private static final Long CREATED_BY_USER_ID = 99L;
    private static final String[] ENTITY_TABLE_NAMES = {"tasks"};

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantAId;
    private static Long tenantBId;
    private static Long taskIdInTenantA;

    @BeforeEach
    void setUpTenantsAndTask() {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tenantAId = createTenant(tx, "Tenant A Academy", "admin@tenantA.com", "100 A Street");
        createTenantSequences(tx, tenantAId);

        tenantBId = createTenant(tx, "Tenant B Academy", "admin@tenantB.com", "200 B Avenue");
        createTenantSequences(tx, tenantBId);

        dataCreated = true;
    }

    // ── Setup: create task in Tenant A ─────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Should create a task in tenant A")
    void shouldCreateTask_inTenantA() throws Exception {
        tenantContextHolder.setTenantId(tenantAId);
        String body = """
                {
                    "title": "Tenant A task",
                    "description": "This belongs to tenant A only",
                    "assigneeId": 1,
                    "assigneeType": "EMPLOYEE",
                    "dueDate": "%s",
                    "priority": "HIGH"
                }
                """.formatted(LocalDate.now().plusDays(7));

        MvcResult result = mockMvc.perform(post(BASE_PATH)
                        .with(authentication(createAuthToken(CREATED_BY_USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Tenant A task"))
                .andReturn();

        taskIdInTenantA = ((Number) com.jayway.jsonpath.JsonPath
                .read(result.getResponse().getContentAsString(), "$.id"))
                .longValue();
        assertThat(taskIdInTenantA).isPositive();
    }

    // ── Isolation: tenant B cannot read ────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Tenant B should not see tenant A's task by ID")
    void tenantB_shouldNotSeeTenantATaskById() throws Exception {
        assertThat(taskIdInTenantA).isNotNull();
        tenantContextHolder.setTenantId(tenantBId);

        mockMvc.perform(get(BASE_PATH + "/{id}", taskIdInTenantA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    @Test
    @Order(3)
    @DisplayName("Tenant B should see empty task list")
    void tenantB_shouldSeeEmptyTaskList() throws Exception {
        tenantContextHolder.setTenantId(tenantBId);

        mockMvc.perform(get(BASE_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── Isolation: tenant B cannot modify ──────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Tenant B should not be able to update tenant A's task")
    void tenantB_shouldNotUpdateTenantATask() throws Exception {
        assertThat(taskIdInTenantA).isNotNull();
        tenantContextHolder.setTenantId(tenantBId);

        String body = """
                {
                    "title": "Hijacked title"
                }
                """;

        mockMvc.perform(put(BASE_PATH + "/{id}", taskIdInTenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    @Test
    @Order(5)
    @DisplayName("Tenant B should not be able to complete tenant A's task")
    void tenantB_shouldNotCompleteTenantATask() throws Exception {
        assertThat(taskIdInTenantA).isNotNull();
        tenantContextHolder.setTenantId(tenantBId);

        mockMvc.perform(patch(BASE_PATH + "/{id}/complete", taskIdInTenantA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    @Test
    @Order(6)
    @DisplayName("Tenant B should not be able to delete tenant A's task")
    void tenantB_shouldNotDeleteTenantATask() throws Exception {
        assertThat(taskIdInTenantA).isNotNull();
        tenantContextHolder.setTenantId(tenantBId);

        mockMvc.perform(delete(BASE_PATH + "/{id}", taskIdInTenantA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND));
    }

    // ── Verify: tenant A's task is still intact ────────────────────────

    @Test
    @Order(7)
    @DisplayName("Tenant A's task should still exist after tenant B's attempts")
    void tenantA_taskShouldStillExist() throws Exception {
        assertThat(taskIdInTenantA).isNotNull();
        tenantContextHolder.setTenantId(tenantAId);

        mockMvc.perform(get(BASE_PATH + "/{id}", taskIdInTenantA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskIdInTenantA))
                .andExpect(jsonPath("$.title").value("Tenant A task"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private UsernamePasswordAuthenticationToken createAuthToken(Long userId) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        "test-user", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        token.setDetails(Map.of("userId", userId));
        return token;
    }

    private Long createTenant(TransactionTemplate tx, String orgName, String email, String address) {
        return tx.execute(status -> {
            TenantDataModel tenant = new TenantDataModel();
            tenant.setOrganizationName(orgName);
            tenant.setEmail(email);
            tenant.setAddress(address);
            entityManager.persist(tenant);
            entityManager.flush();
            return tenant.getTenantId();
        });
    }

    private void createTenantSequences(TransactionTemplate tx, Long tid) {
        tx.executeWithoutResult(status -> {
            for (String tableName : ENTITY_TABLE_NAMES) {
                entityManager.createNativeQuery(
                                "INSERT INTO tenant_sequences "
                                        + "(tenant_id, entity_name, next_value, version) "
                                        + "VALUES (:tenantId, :entityName, 1, 0)")
                        .setParameter("tenantId", tid)
                        .setParameter("entityName", tableName)
                        .executeUpdate();
            }
        });
    }
}
