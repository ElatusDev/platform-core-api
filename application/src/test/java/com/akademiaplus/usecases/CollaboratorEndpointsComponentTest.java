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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Component test for collaborator /v1/my/* self-service endpoints.
 *
 * <p>Boots the full Spring context with Testcontainers MariaDB,
 * creates collaborator test data, and verifies profile, classes,
 * courses, and class students endpoints.
 *
 * @author ElatusDev
 * @since 1.0
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Collaborator Endpoints — Component Test")
class CollaboratorEndpointsComponentTest extends AbstractIntegrationTest {

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
    private static Long collaboratorId;
    private static Long courseId;
    private static Long courseEventId;

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
    @DisplayName("Should return 200 with collaborator profile including skills")
    void shouldReturn200_whenGetCollaboratorProfile() throws Exception {
        setCollaboratorContext();

        mockMvc.perform(get(MY_BASE + "/profile")
                        .with(user("teacher@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(collaboratorId))
                .andExpect(jsonPath("$.profileType").value("COLLABORATOR"))
                .andExpect(jsonPath("$.firstName").value("Maria"))
                .andExpect(jsonPath("$.skills").value("Piano, Guitar"));
    }

    @Test
    @Order(2)
    @DisplayName("Should return 200 with updated collaborator profile")
    void shouldReturn200_whenUpdateCollaboratorProfile() throws Exception {
        setCollaboratorContext();

        mockMvc.perform(put(MY_BASE + "/profile")
                        .with(user("teacher@test.com").roles("COLLABORATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Marie\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Marie"));
    }

    // ── Classes ─────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("Should return 200 with collaborator's assigned classes")
    void shouldReturn200_whenGetClasses() throws Exception {
        setCollaboratorContext();

        mockMvc.perform(get(MY_BASE + "/classes")
                        .with(user("teacher@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.eventTitle == 'Weekly Piano')]").exists());
    }

    // ── Courses ─────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("Should return 200 with collaborator's available courses")
    void shouldReturn200_whenGetCourses() throws Exception {
        setCollaboratorContext();

        mockMvc.perform(get(MY_BASE + "/courses")
                        .with(user("teacher@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.courseName == 'Piano 101')]").exists());
    }

    // ── Class Students ──────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("Should return 200 with students attending the class")
    void shouldReturn200_whenGetClassStudents() throws Exception {
        setCollaboratorContext();

        mockMvc.perform(get(MY_BASE + "/classes/" + courseEventId + "/students")
                        .with(user("teacher@test.com").roles("COLLABORATOR"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void setCollaboratorContext() {
        tenantContextHolder.setTenantId(tenantId);
        userContextHolder.set("COLLABORATOR", collaboratorId);
    }

    private Long createTenant(TransactionTemplate tx) {
        return tx.execute(status -> {
            TenantDataModel tenant = new TenantDataModel();
            tenant.setOrganizationName("Collaborator Test Academy");
            tenant.setEmail("admin@collabtest.com");
            tenant.setAddress("200 Test St");
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

            // Collaborator
            InternalAuthDataModel collabAuth = createInternalAuth("teacher@test.com", "pass123", "COLLABORATOR");
            PersonPIIDataModel collabPii = createPii("Maria", "Teacher", "teacher@test.com");
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            collaborator.setTenantId(tenantId);
            collaborator.setPersonPII(collabPii);
            collaborator.setPersonPiiId(collabPii.getPersonPiiId());
            collaborator.setInternalAuthId(collabAuth.getInternalAuthId());
            collaborator.setSkills("Piano, Guitar");
            collaborator.setBirthDate(LocalDate.of(1985, 5, 10));
            collaborator.setEntryDate(LocalDate.of(2024, 1, 1));
            entityManager.persist(collaborator);
            entityManager.flush();
            collaboratorId = collaborator.getCollaboratorId();

            // Course
            CourseDataModel course = new CourseDataModel();
            course.setTenantId(tenantId);
            course.setCourseName("Piano 101");
            course.setCourseDescription("Beginner piano");
            course.setMaxCapacity(15);
            entityManager.persist(course);
            entityManager.flush();
            courseId = course.getCourseId();

            // Link collaborator to course (M:N via native SQL)
            entityManager.createNativeQuery(
                            "INSERT INTO course_available_collaborators "
                                    + "(tenant_id, course_id, collaborator_id) "
                                    + "VALUES (:tenantId, :courseId, :collaboratorId)")
                    .setParameter("tenantId", tenantId)
                    .setParameter("courseId", courseId)
                    .setParameter("collaboratorId", collaboratorId)
                    .executeUpdate();

            // Schedule
            ScheduleDataModel schedule = new ScheduleDataModel();
            schedule.setTenantId(tenantId);
            schedule.setCourseId(courseId);
            schedule.setScheduleDay("Monday");
            schedule.setStartTime(LocalTime.of(10, 0));
            schedule.setEndTime(LocalTime.of(11, 30));
            entityManager.persist(schedule);
            entityManager.flush();

            // Course Event (Class)
            CourseEventDataModel event = new CourseEventDataModel();
            event.setTenantId(tenantId);
            event.setCourseId(courseId);
            event.setCollaboratorId(collaboratorId);
            event.setEventDate(LocalDate.of(2026, 3, 15));
            event.setEventTitle("Weekly Piano");
            event.setEventDescription("Weekly piano class");
            event.setScheduleId(schedule.getScheduleId());
            entityManager.persist(event);
            entityManager.flush();
            courseEventId = event.getCourseEventId();

            // Adult Student attendee
            CustomerAuthDataModel studentAuth = createCustomerAuth("INTERNAL", "token_student_collab");
            PersonPIIDataModel studentPii = createPii("John", "Student", "john@collabtest.com");
            AdultStudentDataModel adultStudent = new AdultStudentDataModel();
            adultStudent.setTenantId(tenantId);
            adultStudent.setPersonPII(studentPii);
            adultStudent.setPersonPiiId(studentPii.getPersonPiiId());
            adultStudent.setCustomerAuthId(studentAuth.getCustomerAuthId());
            adultStudent.setBirthDate(LocalDate.of(1998, 8, 20));
            adultStudent.setEntryDate(LocalDate.of(2025, 1, 1));
            entityManager.persist(adultStudent);
            entityManager.flush();

            entityManager.createNativeQuery(
                            "INSERT INTO course_event_adult_student_attendees "
                                    + "(tenant_id, course_event_id, adult_student_id) "
                                    + "VALUES (:tenantId, :eventId, :studentId)")
                    .setParameter("tenantId", tenantId)
                    .setParameter("eventId", courseEventId)
                    .setParameter("studentId", adultStudent.getAdultStudentId())
                    .executeUpdate();

            // Tutor (parent of minor student)
            CustomerAuthDataModel tutorAuth = createCustomerAuth("INTERNAL", "token_tutor_collab");
            PersonPIIDataModel tutorPii = createPii("Bob", "Parent", "bob@collabtest.com");
            TutorDataModel tutor = new TutorDataModel();
            tutor.setTenantId(tenantId);
            tutor.setPersonPII(tutorPii);
            tutor.setPersonPiiId(tutorPii.getPersonPiiId());
            tutor.setCustomerAuthId(tutorAuth.getCustomerAuthId());
            tutor.setBirthDate(LocalDate.of(1980, 1, 1));
            tutor.setEntryDate(LocalDate.of(2025, 1, 1));
            entityManager.persist(tutor);
            entityManager.flush();

            // Minor Student attendee
            CustomerAuthDataModel minorAuth = createCustomerAuth("INTERNAL", "token_minor_collab");
            PersonPIIDataModel minorPii = createPii("Emma", "Minor", "emma@collabtest.com");
            MinorStudentDataModel minorStudent = new MinorStudentDataModel();
            minorStudent.setTenantId(tenantId);
            minorStudent.setPersonPII(minorPii);
            minorStudent.setPersonPiiId(minorPii.getPersonPiiId());
            minorStudent.setCustomerAuthId(minorAuth.getCustomerAuthId());
            minorStudent.setTutorId(tutor.getTutorId());
            minorStudent.setBirthDate(LocalDate.of(2015, 3, 5));
            minorStudent.setEntryDate(LocalDate.of(2025, 9, 1));
            entityManager.persist(minorStudent);
            entityManager.flush();

            entityManager.createNativeQuery(
                            "INSERT INTO course_event_minor_student_attendees "
                                    + "(tenant_id, course_event_id, minor_student_id) "
                                    + "VALUES (:tenantId, :eventId, :studentId)")
                    .setParameter("tenantId", tenantId)
                    .setParameter("eventId", courseEventId)
                    .setParameter("studentId", minorStudent.getMinorStudentId())
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
