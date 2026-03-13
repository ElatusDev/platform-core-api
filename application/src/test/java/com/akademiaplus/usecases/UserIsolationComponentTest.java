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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * User isolation test verifying cross-user data never leaks.
 *
 * <p>Creates two adult students (A and B) with separate payments and memberships,
 * and a tutor with minor students. Verifies each user only sees their own data.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("User Isolation — Component Test")
class UserIsolationComponentTest extends AbstractIntegrationTest {

    private static final String MY_BASE = "/v1/my";

    private static final String[] ENTITY_TABLE_NAMES = {
            "person_piis", "customer_auths", "adult_students", "tutors", "minor_students",
            "courses", "memberships", "membership_adult_students",
            "payment_adult_students"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private UserContextHolder userContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long studentAId;
    private static Long studentBId;
    private static Long tutorId;
    private static Long minorStudentId;

    @BeforeEach
    void setUpTestDataOnce() {
        if (dataCreated) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        cleanEntityTables(tx);
        tenantId = createTenant(tx);
        createTenantSequences(tx);
        tenantContextHolder.setTenantId(tenantId);
        createTestData(tx);
        dataCreated = true;
    }

    // ── Payment Isolation ──────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Student A should only see own payments (cash)")
    void studentA_shouldOnlySeeOwnPayments() throws Exception {
        tenantContextHolder.setTenantId(tenantId);
        userContextHolder.set("ADULT_STUDENT", studentAId);

        mockMvc.perform(get(MY_BASE + "/payments")
                        .with(user("alice@iso.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].paymentMethod").value("cash"));
    }

    @Test
    @Order(2)
    @DisplayName("Student B should only see own payments (credit_card)")
    void studentB_shouldOnlySeeOwnPayments() throws Exception {
        tenantContextHolder.setTenantId(tenantId);
        userContextHolder.set("ADULT_STUDENT", studentBId);

        mockMvc.perform(get(MY_BASE + "/payments")
                        .with(user("bob@iso.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].paymentMethod").value("credit_card"));
    }

    // ── Course Isolation ───────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Student A should see own courses")
    void studentA_shouldSeeOwnCourses() throws Exception {
        tenantContextHolder.setTenantId(tenantId);
        userContextHolder.set("ADULT_STUDENT", studentAId);

        mockMvc.perform(get(MY_BASE + "/courses")
                        .with(user("alice@iso.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseName").value("Piano 101"));
    }

    @Test
    @Order(4)
    @DisplayName("Student B should see own courses")
    void studentB_shouldSeeOwnCourses() throws Exception {
        tenantContextHolder.setTenantId(tenantId);
        userContextHolder.set("ADULT_STUDENT", studentBId);

        mockMvc.perform(get(MY_BASE + "/courses")
                        .with(user("bob@iso.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseName").value("Piano 101"));
    }

    // ── Children Isolation ─────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Tutor should only see own children")
    void tutor_shouldOnlySeeOwnChildren() throws Exception {
        tenantContextHolder.setTenantId(tenantId);
        userContextHolder.set("TUTOR", tutorId);

        mockMvc.perform(get(MY_BASE + "/children")
                        .with(user("carlos@iso.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("Diana"));
    }

    @Test
    @Order(6)
    @DisplayName("Tutor should not access child that doesn't belong to them")
    void tutor_shouldNotAccessOtherTutorsChild() throws Exception {
        tenantContextHolder.setTenantId(tenantId);
        userContextHolder.set("TUTOR", tutorId);

        mockMvc.perform(get(MY_BASE + "/children/999999/courses")
                        .with(user("carlos@iso.com").roles("CUSTOMER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void cleanEntityTables(TransactionTemplate tx) {
        tx.executeWithoutResult(status -> {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
            for (String table : ENTITY_TABLE_NAMES) {
                entityManager.createNativeQuery("DELETE FROM " + table).executeUpdate();
            }
            entityManager.createNativeQuery("DELETE FROM tenant_sequences").executeUpdate();
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        });
    }

    private Long createTenant(TransactionTemplate tx) {
        return tx.execute(status -> {
            TenantDataModel tenant = new TenantDataModel();
            tenant.setOrganizationName("Isolation Test Academy");
            tenant.setEmail("admin@isolation.com");
            tenant.setAddress("200 Isolation Blvd");
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

            // Student A
            CustomerAuthDataModel authA = createCustomerAuth("INTERNAL", "token_a");
            PersonPIIDataModel piiA = createPii("Alice", "StudentA", "alice@iso.com");
            AdultStudentDataModel studentA = new AdultStudentDataModel();
            studentA.setTenantId(tenantId);
            studentA.setPersonPII(piiA);
            studentA.setPersonPiiId(piiA.getPersonPiiId());
            studentA.setCustomerAuthId(authA.getCustomerAuthId());
            studentA.setBirthDate(LocalDate.of(1993, 1, 1));
            studentA.setEntryDate(LocalDate.of(2025, 1, 1));
            entityManager.persist(studentA);
            entityManager.flush();
            studentAId = studentA.getAdultStudentId();

            // Student B
            CustomerAuthDataModel authB = createCustomerAuth("INTERNAL", "token_b");
            PersonPIIDataModel piiB = createPii("Bob", "StudentB", "bob@iso.com");
            AdultStudentDataModel studentB = new AdultStudentDataModel();
            studentB.setTenantId(tenantId);
            studentB.setPersonPII(piiB);
            studentB.setPersonPiiId(piiB.getPersonPiiId());
            studentB.setCustomerAuthId(authB.getCustomerAuthId());
            studentB.setBirthDate(LocalDate.of(1994, 2, 2));
            studentB.setEntryDate(LocalDate.of(2025, 2, 1));
            entityManager.persist(studentB);
            entityManager.flush();
            studentBId = studentB.getAdultStudentId();

            // Course
            CourseDataModel course = new CourseDataModel();
            course.setTenantId(tenantId);
            course.setCourseName("Piano 101");
            course.setCourseDescription("Beginner piano");
            course.setMaxCapacity(15);
            entityManager.persist(course);
            entityManager.flush();

            // Membership type
            MembershipDataModel membership = new MembershipDataModel();
            membership.setTenantId(tenantId);
            membership.setMembershipType("MONTHLY");
            membership.setFee(new BigDecimal("29.99"));
            membership.setDescription("Monthly");
            entityManager.persist(membership);
            entityManager.flush();

            // Membership + Payment for Student A
            MembershipAdultStudentDataModel masA = new MembershipAdultStudentDataModel();
            masA.setTenantId(tenantId);
            masA.setAdultStudentId(studentAId);
            masA.setMembershipId(membership.getMembershipId());
            masA.setCourseId(course.getCourseId());
            masA.setStartDate(LocalDate.of(2026, 1, 1));
            masA.setDueDate(LocalDate.of(2026, 2, 1));
            entityManager.persist(masA);
            entityManager.flush();

            PaymentAdultStudentDataModel payA = new PaymentAdultStudentDataModel();
            payA.setTenantId(tenantId);
            payA.setMembershipAdultStudentId(masA.getMembershipAdultStudentId());
            payA.setAmount(new BigDecimal("29.99"));
            payA.setPaymentDate(LocalDate.of(2026, 1, 10));
            payA.setPaymentMethod("cash");
            entityManager.persist(payA);
            entityManager.flush();

            // Membership + Payment for Student B
            MembershipAdultStudentDataModel masB = new MembershipAdultStudentDataModel();
            masB.setTenantId(tenantId);
            masB.setAdultStudentId(studentBId);
            masB.setMembershipId(membership.getMembershipId());
            masB.setCourseId(course.getCourseId());
            masB.setStartDate(LocalDate.of(2026, 3, 1));
            masB.setDueDate(LocalDate.of(2026, 4, 1));
            entityManager.persist(masB);
            entityManager.flush();

            PaymentAdultStudentDataModel payB = new PaymentAdultStudentDataModel();
            payB.setTenantId(tenantId);
            payB.setMembershipAdultStudentId(masB.getMembershipAdultStudentId());
            payB.setAmount(new BigDecimal("29.99"));
            payB.setPaymentDate(LocalDate.of(2026, 3, 10));
            payB.setPaymentMethod("credit_card");
            entityManager.persist(payB);
            entityManager.flush();

            // Tutor with minor student
            CustomerAuthDataModel authT = createCustomerAuth("INTERNAL", "token_tutor");
            PersonPIIDataModel tutorPii = createPii("Carlos", "Tutor", "carlos@iso.com");
            TutorDataModel tutor = new TutorDataModel();
            tutor.setTenantId(tenantId);
            tutor.setPersonPII(tutorPii);
            tutor.setPersonPiiId(tutorPii.getPersonPiiId());
            tutor.setCustomerAuthId(authT.getCustomerAuthId());
            tutor.setBirthDate(LocalDate.of(1980, 5, 5));
            tutor.setEntryDate(LocalDate.of(2024, 1, 1));
            entityManager.persist(tutor);
            entityManager.flush();
            tutorId = tutor.getTutorId();

            CustomerAuthDataModel authC = createCustomerAuth("INTERNAL", "token_child");
            PersonPIIDataModel childPii = createPii("Diana", "Child", "diana@iso.com");
            MinorStudentDataModel child = new MinorStudentDataModel();
            child.setTenantId(tenantId);
            child.setPersonPII(childPii);
            child.setPersonPiiId(childPii.getPersonPiiId());
            child.setCustomerAuthId(authC.getCustomerAuthId());
            child.setTutorId(tutorId);
            child.setBirthDate(LocalDate.of(2014, 8, 8));
            child.setEntryDate(LocalDate.of(2025, 9, 1));
            entityManager.persist(child);
            entityManager.flush();
            minorStudentId = child.getMinorStudentId();
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
