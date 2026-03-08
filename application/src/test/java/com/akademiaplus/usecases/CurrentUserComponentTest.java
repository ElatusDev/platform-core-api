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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component tests for the current user /me endpoint.
 *
 * <p>Full Spring context with Testcontainers MariaDB. Creates an employee
 * via the API, then sets the SecurityContext manually (JWT filter is disabled
 * in tests) to verify the /me endpoint resolves the correct profile.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("CurrentUserComponentTest")
@AutoConfigureMockMvc(addFilters = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CurrentUserComponentTest extends AbstractIntegrationTest {

    public static final String ME_PATH = "/v1/user-management/me";
    public static final String EMPLOYEES_PATH = "/v1/user-management/employees";

    public static final String TENANT_ORG_NAME = "CurrentUser Test Academy";
    public static final String TENANT_EMAIL = "admin@currentusertest.com";
    public static final String TENANT_ADDRESS = "200 Me Blvd";

    public static final String EMPLOYEE_TYPE = "FULL_TIME";
    public static final String EMPLOYEE_ROLE = "ADMIN";
    public static final String EMPLOYEE_BIRTHDATE = "1985-03-20";
    public static final String EMPLOYEE_ENTRY_DATE = "2021-01-10";

    public static final String EMP_FIRST_NAME = "Sofia";
    public static final String EMP_LAST_NAME = "Ramirez";
    public static final String EMP_EMAIL = "sofia.ramirez@currentusertest.com";
    public static final String EMP_PHONE = "+525511112222";
    public static final String EMP_ADDRESS = "789 Test St";
    public static final String EMP_ZIP_CODE = "20000";
    public static final String EMP_USERNAME = "sofiaramirez";
    public static final String EMP_PASSWORD = "SofiaPass1!";

    private static final String[] ENTITY_TABLE_NAMES = {
            "employees", "person_piis", "internal_auths"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;

    @BeforeEach
    void setUpTestDataOnce() throws Exception {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tenantId = createTenant(tx);
        createTenantSequences(tx);
        tenantContextHolder.setTenantId(tenantId);
        createEmployeeViaApi();
        dataCreated = true;
    }

    @Test
    @Order(1)
    @DisplayName("Should return 200 with employee profile when authenticated user calls /me")
    void shouldReturn200_withEmployeeProfile_whenAuthenticated() throws Exception {
        // Given — set SecurityContext with the employee's username
        tenantContextHolder.setTenantId(tenantId);
        setSecurityContext(EMP_USERNAME);

        // When / Then
        mockMvc.perform(get(ME_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userType").value("EMPLOYEE"))
                .andExpect(jsonPath("$.username").value(EMP_USERNAME))
                .andExpect(jsonPath("$.firstName").value(EMP_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(EMP_LAST_NAME))
                .andExpect(jsonPath("$.email").value(EMP_EMAIL))
                .andExpect(jsonPath("$.role").value(EMPLOYEE_ROLE))
                .andExpect(jsonPath("$.employeeId").exists())
                .andExpect(jsonPath("$.internalAuthId").isNumber());
    }

    @Test
    @Order(2)
    @DisplayName("Should return 404 when authenticated user has no profile")
    void shouldReturn404_whenUserHasNoProfile() throws Exception {
        // Given — set SecurityContext with a username that has no employee/collaborator record
        tenantContextHolder.setTenantId(tenantId);
        setSecurityContext("nonexistent-user");

        // When / Then
        mockMvc.perform(get(ME_PATH))
                .andExpect(status().isNotFound());
    }

    private void createEmployeeViaApi() throws Exception {
        String body = """
                {
                    "employeeType": "%s",
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
                """.formatted(EMPLOYEE_TYPE, EMPLOYEE_BIRTHDATE, EMPLOYEE_ENTRY_DATE,
                EMP_FIRST_NAME, EMP_LAST_NAME, EMP_EMAIL, EMP_PHONE,
                EMP_ADDRESS, EMP_ZIP_CODE, EMP_USERNAME, EMP_PASSWORD, EMPLOYEE_ROLE);

        mockMvc.perform(post(EMPLOYEES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private void setSecurityContext(String username) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

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
