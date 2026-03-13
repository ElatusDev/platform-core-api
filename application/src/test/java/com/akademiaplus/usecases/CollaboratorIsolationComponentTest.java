/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.config.AbstractIntegrationTest;
import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
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

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Isolation test verifying cross-collaborator data boundaries.
 *
 * <p>Creates two collaborators (A and B) each with their own courses,
 * classes, and students. Verifies that each collaborator can only
 * see their own data.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Collaborator Isolation — Component Test")
class CollaboratorIsolationComponentTest extends AbstractIntegrationTest {

    private static final String MY_BASE = "/v1/my";

    private static final String[] ENTITY_TABLE_NAMES = {
            "person_piis", "internal_auths", "collaborators",
            "courses", "schedules", "course_events",
            "customer_auths", "adult_students", "minor_students", "tutors"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private TenantContextHolder tenantContextHolder;
    @Autowired private UserContextHolder userContextHolder;
    @Autowired private PlatformTransactionManager transactionManager;

    private static boolean dataCreated;
    private static Long tenantId;
    private static Long collaboratorAId;
    private static Long collaboratorBId;
    private static Long courseAId;
    private static Long courseBId;
    private static Long classAId;
    private static Long classBId;

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

    // ── Classes Isolation ───────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Collaborator A sees only class A")
    void collaboratorA_seesOnlyClassA() throws Exception {
        setContext(collaboratorAId);

        mockMvc.perform(get(MY_BASE + "/classes")
                        .with(user("collabA@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].classId").value(classAId));
    }

    @Test
    @Order(2)
    @DisplayName("Collaborator B sees only class B")
    void collaboratorB_seesOnlyClassB() throws Exception {
        setContext(collaboratorBId);

        mockMvc.perform(get(MY_BASE + "/classes")
                        .with(user("collabB@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].classId").value(classBId));
    }

    // ── Courses Isolation ───────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Collaborator A sees only course A")
    void collaboratorA_seesOnlyCourseA() throws Exception {
        setContext(collaboratorAId);

        mockMvc.perform(get(MY_BASE + "/courses")
                        .with(user("collabA@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseName").value("Course A"));
    }

    @Test
    @Order(4)
    @DisplayName("Collaborator B sees only course B")
    void collaboratorB_seesOnlyCourseB() throws Exception {
        setContext(collaboratorBId);

        mockMvc.perform(get(MY_BASE + "/courses")
                        .with(user("collabB@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseName").value("Course B"));
    }

    // ── Students Isolation ──────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Collaborator A sees students in class A")
    void collaboratorA_seesStudentsInClassA() throws Exception {
        setContext(collaboratorAId);

        mockMvc.perform(get(MY_BASE + "/classes/" + classAId + "/students")
                        .with(user("collabA@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("StudentX"));
    }

    @Test
    @Order(6)
    @DisplayName("Collaborator A gets 404 for class B students")
    void collaboratorA_gets404ForClassBStudents() throws Exception {
        setContext(collaboratorAId);

        mockMvc.perform(get(MY_BASE + "/classes/" + classBId + "/students")
                        .with(user("collabA@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    @DisplayName("Collaborator B sees students in class B")
    void collaboratorB_seesStudentsInClassB() throws Exception {
        setContext(collaboratorBId);

        mockMvc.perform(get(MY_BASE + "/classes/" + classBId + "/students")
                        .with(user("collabB@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("StudentY"));
    }

    @Test
    @Order(8)
    @DisplayName("Collaborator B gets 404 for class A students")
    void collaboratorB_gets404ForClassAStudents() throws Exception {
        setContext(collaboratorBId);

        mockMvc.perform(get(MY_BASE + "/classes/" + classAId + "/students")
                        .with(user("collabB@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void setContext(Long collaboratorId) {
        tenantContextHolder.setTenantId(tenantId);
        userContextHolder.set("COLLABORATOR", collaboratorId);
    }

    private Long createTenant(TransactionTemplate tx) {
        return tx.execute(status -> {
            TenantDataModel tenant = new TenantDataModel();
            tenant.setOrganizationName("Isolation Test Academy");
            tenant.setEmail("admin@isolation.com");
            tenant.setAddress("300 Test St");
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

            // ── Collaborator A ──
            InternalAuthDataModel authA = createInternalAuth("collabA@test.com", "passA", "COLLABORATOR");
            PersonPIIDataModel piiA = createPii("CollabA", "Teacher", "collabA@test.com");
            CollaboratorDataModel collabA = new CollaboratorDataModel();
            collabA.setTenantId(tenantId);
            collabA.setPersonPII(piiA);
            collabA.setPersonPiiId(piiA.getPersonPiiId());
            collabA.setInternalAuthId(authA.getInternalAuthId());
            collabA.setSkills("Piano");
            collabA.setBirthDate(LocalDate.of(1985, 1, 1));
            collabA.setEntryDate(LocalDate.of(2024, 1, 1));
            entityManager.persist(collabA);
            entityManager.flush();
            collaboratorAId = collabA.getCollaboratorId();

            // ── Collaborator B ──
            InternalAuthDataModel authB = createInternalAuth("collabB@test.com", "passB", "COLLABORATOR");
            PersonPIIDataModel piiB = createPii("CollabB", "Teacher", "collabB@test.com");
            CollaboratorDataModel collabB = new CollaboratorDataModel();
            collabB.setTenantId(tenantId);
            collabB.setPersonPII(piiB);
            collabB.setPersonPiiId(piiB.getPersonPiiId());
            collabB.setInternalAuthId(authB.getInternalAuthId());
            collabB.setSkills("Guitar");
            collabB.setBirthDate(LocalDate.of(1990, 6, 15));
            collabB.setEntryDate(LocalDate.of(2024, 6, 1));
            entityManager.persist(collabB);
            entityManager.flush();
            collaboratorBId = collabB.getCollaboratorId();

            // ── Course A (available to collaborator A only) ──
            CourseDataModel courseA = new CourseDataModel();
            courseA.setTenantId(tenantId);
            courseA.setCourseName("Course A");
            courseA.setCourseDescription("Course for collab A");
            courseA.setMaxCapacity(10);
            entityManager.persist(courseA);
            entityManager.flush();
            courseAId = courseA.getCourseId();

            entityManager.createNativeQuery(
                            "INSERT INTO course_available_collaborators "
                                    + "(tenant_id, course_id, collaborator_id) "
                                    + "VALUES (:tenantId, :courseId, :collaboratorId)")
                    .setParameter("tenantId", tenantId)
                    .setParameter("courseId", courseAId)
                    .setParameter("collaboratorId", collaboratorAId)
                    .executeUpdate();

            // ── Course B (available to collaborator B only) ──
            CourseDataModel courseB = new CourseDataModel();
            courseB.setTenantId(tenantId);
            courseB.setCourseName("Course B");
            courseB.setCourseDescription("Course for collab B");
            courseB.setMaxCapacity(10);
            entityManager.persist(courseB);
            entityManager.flush();
            courseBId = courseB.getCourseId();

            entityManager.createNativeQuery(
                            "INSERT INTO course_available_collaborators "
                                    + "(tenant_id, course_id, collaborator_id) "
                                    + "VALUES (:tenantId, :courseId, :collaboratorId)")
                    .setParameter("tenantId", tenantId)
                    .setParameter("courseId", courseBId)
                    .setParameter("collaboratorId", collaboratorBId)
                    .executeUpdate();

            // ── Schedules ──
            ScheduleDataModel scheduleA = new ScheduleDataModel();
            scheduleA.setTenantId(tenantId);
            scheduleA.setCourseId(courseAId);
            scheduleA.setScheduleDay("Monday");
            scheduleA.setStartTime(LocalTime.of(9, 0));
            scheduleA.setEndTime(LocalTime.of(10, 0));
            entityManager.persist(scheduleA);
            entityManager.flush();

            ScheduleDataModel scheduleB = new ScheduleDataModel();
            scheduleB.setTenantId(tenantId);
            scheduleB.setCourseId(courseBId);
            scheduleB.setScheduleDay("Tuesday");
            scheduleB.setStartTime(LocalTime.of(14, 0));
            scheduleB.setEndTime(LocalTime.of(15, 0));
            entityManager.persist(scheduleB);
            entityManager.flush();

            // ── Class A (assigned to collaborator A) ──
            CourseEventDataModel eventA = new CourseEventDataModel();
            eventA.setTenantId(tenantId);
            eventA.setCourseId(courseAId);
            eventA.setCollaboratorId(collaboratorAId);
            eventA.setEventDate(LocalDate.of(2026, 3, 15));
            eventA.setEventTitle("Class A");
            eventA.setEventDescription("Class for collab A");
            eventA.setScheduleId(scheduleA.getScheduleId());
            entityManager.persist(eventA);
            entityManager.flush();
            classAId = eventA.getCourseEventId();

            // ── Class B (assigned to collaborator B) ──
            CourseEventDataModel eventB = new CourseEventDataModel();
            eventB.setTenantId(tenantId);
            eventB.setCourseId(courseBId);
            eventB.setCollaboratorId(collaboratorBId);
            eventB.setEventDate(LocalDate.of(2026, 3, 16));
            eventB.setEventTitle("Class B");
            eventB.setEventDescription("Class for collab B");
            eventB.setScheduleId(scheduleB.getScheduleId());
            entityManager.persist(eventB);
            entityManager.flush();
            classBId = eventB.getCourseEventId();

            // ── Tutor (parent for minor students) ──
            CustomerAuthDataModel tutorAuth = createCustomerAuth("INTERNAL", "token_tutor_iso");
            PersonPIIDataModel tutorPii = createPii("Parent", "Iso", "parent@isolation.com");
            TutorDataModel tutor = new TutorDataModel();
            tutor.setTenantId(tenantId);
            tutor.setPersonPII(tutorPii);
            tutor.setPersonPiiId(tutorPii.getPersonPiiId());
            tutor.setCustomerAuthId(tutorAuth.getCustomerAuthId());
            tutor.setBirthDate(LocalDate.of(1980, 1, 1));
            tutor.setEntryDate(LocalDate.of(2025, 1, 1));
            entityManager.persist(tutor);
            entityManager.flush();

            // ── Student X (attends class A) ──
            CustomerAuthDataModel authX = createCustomerAuth("INTERNAL", "token_x");
            PersonPIIDataModel piiX = createPii("StudentX", "Last", "studentx@isolation.com");
            AdultStudentDataModel studentX = new AdultStudentDataModel();
            studentX.setTenantId(tenantId);
            studentX.setPersonPII(piiX);
            studentX.setPersonPiiId(piiX.getPersonPiiId());
            studentX.setCustomerAuthId(authX.getCustomerAuthId());
            studentX.setBirthDate(LocalDate.of(2000, 1, 1));
            studentX.setEntryDate(LocalDate.of(2025, 1, 1));
            entityManager.persist(studentX);
            entityManager.flush();

            entityManager.createNativeQuery(
                            "INSERT INTO course_event_adult_student_attendees "
                                    + "(tenant_id, course_event_id, adult_student_id) "
                                    + "VALUES (:tenantId, :eventId, :studentId)")
                    .setParameter("tenantId", tenantId)
                    .setParameter("eventId", classAId)
                    .setParameter("studentId", studentX.getAdultStudentId())
                    .executeUpdate();

            // ── Student Y (attends class B) ──
            CustomerAuthDataModel authY = createCustomerAuth("INTERNAL", "token_y");
            PersonPIIDataModel piiY = createPii("StudentY", "Last", "studenty@isolation.com");
            AdultStudentDataModel studentY = new AdultStudentDataModel();
            studentY.setTenantId(tenantId);
            studentY.setPersonPII(piiY);
            studentY.setPersonPiiId(piiY.getPersonPiiId());
            studentY.setCustomerAuthId(authY.getCustomerAuthId());
            studentY.setBirthDate(LocalDate.of(2001, 2, 2));
            studentY.setEntryDate(LocalDate.of(2025, 2, 1));
            entityManager.persist(studentY);
            entityManager.flush();

            entityManager.createNativeQuery(
                            "INSERT INTO course_event_adult_student_attendees "
                                    + "(tenant_id, course_event_id, adult_student_id) "
                                    + "VALUES (:tenantId, :eventId, :studentId)")
                    .setParameter("tenantId", tenantId)
                    .setParameter("eventId", classBId)
                    .setParameter("studentId", studentY.getAdultStudentId())
                    .executeUpdate();
        });
    }

    private InternalAuthDataModel createInternalAuth(String username, String password, String role) {
        InternalAuthDataModel auth = new InternalAuthDataModel();
        auth.setTenantId(tenantId);
        auth.setUsername(username);
        auth.setUsernameHash("hash_" + username);
        auth.setPassword(password);
        auth.setRole(role);
        entityManager.persist(auth);
        entityManager.flush();
        return auth;
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
