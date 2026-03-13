/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for /v1/my/* self-service endpoints.
 *
 * <p>Boots the full Spring context with Testcontainers MariaDB,
 * creates tenant data, and verifies all 8 endpoints.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("My Endpoints — Component Test")
class MyEndpointsComponentTest extends AbstractIntegrationTest {

    private static final String MY_BASE = "/v1/my";

    private static final String[] ENTITY_TABLE_NAMES = {
            "person_piis", "customer_auths", "adult_students", "tutors", "minor_students",
            "courses", "schedules", "memberships", "membership_adult_students",
            "payment_adult_students"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private UserContextHolder userContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long adultStudentId;
    private static Long tutorId;
    private static Long minorStudentId;
    private static Long courseId;

    @BeforeEach
    void setUpTestDataOnce() {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tenantId = createTenant(tx);
        createTenantSequences(tx);
        tenantContextHolder.setTenantId(tenantId);
        createTestData(tx);
        dataCreated = true;
    }

    // ── Profile ─────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Should return 200 with student profile")
    void shouldReturn200_whenGetProfile() throws Exception {
        setStudentContext();

        mockMvc.perform(get(MY_BASE + "/profile")
                        .with(user("jane@test.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(adultStudentId))
                .andExpect(jsonPath("$.profileType").value("ADULT_STUDENT"))
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    @Order(2)
    @DisplayName("Should return 200 with updated profile")
    void shouldReturn200_whenUpdateProfile() throws Exception {
        setStudentContext();

        mockMvc.perform(put(MY_BASE + "/profile")
                        .with(user("jane@test.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Janet\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Janet"));
    }

    // ── Courses ─────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with courses list")
    void shouldReturn200_whenGetCourses() throws Exception {
        setStudentContext();

        mockMvc.perform(get(MY_BASE + "/courses")
                        .with(user("jane@test.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].courseName").value("Guitar 101"));
    }

    // ── Schedule ────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Should return 200 with schedule")
    void shouldReturn200_whenGetSchedule() throws Exception {
        setStudentContext();

        mockMvc.perform(get(MY_BASE + "/schedule")
                        .with(user("jane@test.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].scheduleDay").value("Monday"));
    }

    // ── Memberships ────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Should return 200 with memberships")
    void shouldReturn200_whenGetMemberships() throws Exception {
        setStudentContext();

        mockMvc.perform(get(MY_BASE + "/memberships")
                        .with(user("jane@test.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].membershipType").value("MONTHLY"));
    }

    // ── Payments ───────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("Should return 200 with payments")
    void shouldReturn200_whenGetPayments() throws Exception {
        setStudentContext();

        mockMvc.perform(get(MY_BASE + "/payments")
                        .with(user("jane@test.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].paymentMethod").value("credit_card"));
    }

    // ── Children ───────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("Should return 200 with tutor's children")
    void shouldReturn200_whenGetChildren() throws Exception {
        setTutorContext();

        mockMvc.perform(get(MY_BASE + "/children")
                        .with(user("bob@test.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].firstName").value("Alice"));
    }

    @Test
    @Order(8)
    @DisplayName("Should return 200 with child courses")
    void shouldReturn200_whenGetChildCourses() throws Exception {
        setTutorContext();

        mockMvc.perform(get(MY_BASE + "/children/" + minorStudentId + "/courses")
                        .with(user("bob@test.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void setStudentContext() {
        tenantContextHolder.setTenantId(tenantId);
        userContextHolder.set("ADULT_STUDENT", adultStudentId);
    }

    private void setTutorContext() {
        tenantContextHolder.setTenantId(tenantId);
        userContextHolder.set("TUTOR", tutorId);
    }

    private Long createTenant(TransactionTemplate tx) {
        return tx.execute(status -> {
            TenantDataModel tenant = new TenantDataModel();
            tenant.setOrganizationName("My Endpoints Test Academy");
            tenant.setEmail("admin@mytest.com");
            tenant.setAddress("100 Test St");
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

    private void createTestData(TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            tenantContextHolder.setTenantId(tenantId);

            CustomerAuthDataModel studentAuth = createCustomerAuth("INTERNAL", "token_student");
            PersonPIIDataModel studentPii = createPii("Jane", "Student", "jane@mytest.com");
            AdultStudentDataModel student = new AdultStudentDataModel();
            student.setTenantId(tenantId);
            student.setPersonPII(studentPii);
            student.setPersonPiiId(studentPii.getPersonPiiId());
            student.setCustomerAuthId(studentAuth.getCustomerAuthId());
            student.setBirthDate(LocalDate.of(1995, 3, 10));
            student.setEntryDate(LocalDate.of(2025, 1, 1));
            entityManager.persist(student);
            entityManager.flush();
            adultStudentId = student.getAdultStudentId();

            CustomerAuthDataModel tutorAuth = createCustomerAuth("INTERNAL", "token_tutor");
            PersonPIIDataModel tutorPii = createPii("Bob", "Tutor", "bob@mytest.com");
            TutorDataModel tutor = new TutorDataModel();
            tutor.setTenantId(tenantId);
            tutor.setPersonPII(tutorPii);
            tutor.setPersonPiiId(tutorPii.getPersonPiiId());
            tutor.setCustomerAuthId(tutorAuth.getCustomerAuthId());
            tutor.setBirthDate(LocalDate.of(1985, 7, 20));
            tutor.setEntryDate(LocalDate.of(2024, 9, 1));
            entityManager.persist(tutor);
            entityManager.flush();
            tutorId = tutor.getTutorId();

            CustomerAuthDataModel childAuth = createCustomerAuth("INTERNAL", "token_child");
            PersonPIIDataModel childPii = createPii("Alice", "Child", "alice@mytest.com");
            MinorStudentDataModel child = new MinorStudentDataModel();
            child.setTenantId(tenantId);
            child.setPersonPII(childPii);
            child.setPersonPiiId(childPii.getPersonPiiId());
            child.setCustomerAuthId(childAuth.getCustomerAuthId());
            child.setTutorId(tutorId);
            child.setBirthDate(LocalDate.of(2015, 4, 5));
            child.setEntryDate(LocalDate.of(2025, 9, 1));
            entityManager.persist(child);
            entityManager.flush();
            minorStudentId = child.getMinorStudentId();

            CourseDataModel course = new CourseDataModel();
            course.setTenantId(tenantId);
            course.setCourseName("Guitar 101");
            course.setCourseDescription("Beginner guitar");
            course.setMaxCapacity(20);
            entityManager.persist(course);
            entityManager.flush();
            courseId = course.getCourseId();

            ScheduleDataModel schedule = new ScheduleDataModel();
            schedule.setTenantId(tenantId);
            schedule.setCourseId(courseId);
            schedule.setScheduleDay("Monday");
            schedule.setStartTime(LocalTime.of(9, 0));
            schedule.setEndTime(LocalTime.of(10, 30));
            entityManager.persist(schedule);
            entityManager.flush();

            MembershipDataModel membership = new MembershipDataModel();
            membership.setTenantId(tenantId);
            membership.setMembershipType("MONTHLY");
            membership.setFee(new BigDecimal("49.99"));
            membership.setDescription("Monthly membership");
            entityManager.persist(membership);
            entityManager.flush();

            MembershipAdultStudentDataModel mas = new MembershipAdultStudentDataModel();
            mas.setTenantId(tenantId);
            mas.setAdultStudentId(adultStudentId);
            mas.setMembershipId(membership.getMembershipId());
            mas.setCourseId(courseId);
            mas.setStartDate(LocalDate.of(2026, 1, 1));
            mas.setDueDate(LocalDate.of(2026, 2, 1));
            entityManager.persist(mas);
            entityManager.flush();

            PaymentAdultStudentDataModel payment = new PaymentAdultStudentDataModel();
            payment.setTenantId(tenantId);
            payment.setMembershipAdultStudentId(mas.getMembershipAdultStudentId());
            payment.setAmount(new BigDecimal("49.99"));
            payment.setPaymentDate(LocalDate.of(2026, 1, 15));
            payment.setPaymentMethod("credit_card");
            entityManager.persist(payment);
            entityManager.flush();
        });
    }

    private CustomerAuthDataModel createCustomerAuth(String provider, String token) {
        CustomerAuthDataModel auth = new CustomerAuthDataModel();
        auth.setTenantId(tenantId);
        auth.setProvider(provider);
        auth.setToken(token);
        entityManager.persist(auth);
        entityManager.flush();
        return auth;
    }

    private PersonPIIDataModel createPii(String firstName, String lastName, String email) {
        PersonPIIDataModel pii = new PersonPIIDataModel();
        pii.setTenantId(tenantId);
        pii.setFirstName(firstName);
        pii.setLastName(lastName);
        pii.setEmail(email);
        pii.setPhoneNumber("555-0000");
        pii.setAddress("1 Test St");
        pii.setZipCode("00000");
        pii.setEmailHash("hash_" + email);
        pii.setPhoneHash("phone_" + email);
        entityManager.persist(pii);
        entityManager.flush();
        return pii;
    }
}
