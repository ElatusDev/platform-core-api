/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.billing.customerpayment.PaymentTutorDataModel;
import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.billing.payroll.CompensationDataModel;
import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.usecases.billing.LoadCardPaymentInfoMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadCompensationMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadMembershipAdultStudentMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadMembershipMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadMembershipTutorMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadPaymentAdultStudentMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadPaymentTutorMockDataUseCase;
import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.usecases.course.LoadCourseEventMockDataUseCase;
import com.akademiaplus.usecases.course.LoadCourseMockDataUseCase;
import com.akademiaplus.usecases.course.LoadScheduleMockDataUseCase;
import com.akademiaplus.attendance.AttendanceSessionDataModel;
import com.akademiaplus.attendance.interfaceadapters.AttendanceSessionRepository;
import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import com.akademiaplus.notification.interfaceadapters.EmailRepository;
import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.email.EmailDataModel;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import com.akademiaplus.store.interfaceadapters.StoreTransactionRepository;
import com.akademiaplus.usecases.attendance.LoadAttendanceRecordMockDataUseCase;
import com.akademiaplus.usecases.attendance.LoadAttendanceSessionMockDataUseCase;
import com.akademiaplus.usecases.leadmanagement.LoadDemoRequestMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailAttachmentMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailRecipientMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadNotificationDeliveryMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadNotificationMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadNotificationReadStatusMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadPushDeviceMockDataUseCase;
import com.akademiaplus.usecases.store.LoadStoreSaleItemMockDataUseCase;
import com.akademiaplus.usecases.store.LoadStoreProductMockDataUseCase;
import com.akademiaplus.usecases.store.LoadStoreTransactionMockDataUseCase;
import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.tenancy.TenantSubscriptionDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.usecases.billing.LoadCompensationCollaboratorMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadMembershipCourseMockDataUseCase;
import com.akademiaplus.usecases.course.LoadAdultStudentCourseMockDataUseCase;
import com.akademiaplus.usecases.course.LoadCourseAvailableCollaboratorMockDataUseCase;
import com.akademiaplus.usecases.course.LoadCourseEventAdultStudentAttendeeMockDataUseCase;
import com.akademiaplus.usecases.course.LoadCourseEventMinorStudentAttendeeMockDataUseCase;
import com.akademiaplus.usecases.course.LoadMinorStudentCourseMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailTemplateMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailTemplateVariableMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadNewsFeedItemMockDataUseCase;
import com.akademiaplus.usecases.task.LoadTaskMockDataUseCase;
import com.akademiaplus.usecases.tenant.LoadTenantBrandingMockDataUseCase;
import com.akademiaplus.util.mock.billing.CardPaymentInfoFactory;
import com.akademiaplus.util.mock.billing.CompensationCollaboratorFactory;
import com.akademiaplus.util.mock.billing.MembershipAdultStudentFactory;
import com.akademiaplus.util.mock.billing.MembershipCourseFactory;
import com.akademiaplus.util.mock.billing.MembershipTutorFactory;
import com.akademiaplus.util.mock.billing.PaymentAdultStudentFactory;
import com.akademiaplus.util.mock.billing.PaymentTutorFactory;
import com.akademiaplus.util.mock.course.AdultStudentCourseFactory;
import com.akademiaplus.util.mock.course.CourseAvailableCollaboratorFactory;
import com.akademiaplus.util.mock.course.CourseEventAdultStudentAttendeeFactory;
import com.akademiaplus.util.mock.course.CourseEventFactory;
import com.akademiaplus.util.mock.course.CourseEventMinorStudentAttendeeFactory;
import com.akademiaplus.util.mock.course.MinorStudentCourseFactory;
import com.akademiaplus.util.mock.course.ScheduleFactory;
import com.akademiaplus.util.mock.attendance.AttendanceRecordFactory;
import com.akademiaplus.util.mock.attendance.AttendanceSessionFactory;
import com.akademiaplus.util.mock.notification.EmailAttachmentFactory;
import com.akademiaplus.util.mock.notification.EmailRecipientFactory;
import com.akademiaplus.util.mock.notification.EmailTemplateVariableFactory;
import com.akademiaplus.util.mock.notification.NewsFeedItemFactory;
import com.akademiaplus.util.mock.notification.NotificationDeliveryFactory;
import com.akademiaplus.util.mock.notification.NotificationReadStatusFactory;
import com.akademiaplus.util.mock.notification.PushDeviceFactory;
import com.akademiaplus.util.mock.store.StoreSaleItemFactory;
import com.akademiaplus.util.mock.task.TaskFactory;
import com.akademiaplus.util.mock.users.MinorStudentFactory;
import com.akademiaplus.utilities.idgeneration.interfaceadapters.TenantSequence;
import com.akademiaplus.notification.interfaceadapters.EmailTemplateRepository;
import com.akademiaplus.notifications.email.EmailTemplateDataModel;
import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.payroll.interfaceadapters.CompensationRepository;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.usecases.users.LoadAdultStudentMockDataUseCase;
import com.akademiaplus.usecases.users.LoadCollaboratorMockDataUseCase;
import com.akademiaplus.usecases.users.LoadEmployeeMockDataUseCase;
import com.akademiaplus.usecases.users.LoadMinorStudentMockDataUseCase;
import com.akademiaplus.usecases.users.LoadTenantMockDataUseCase;
import com.akademiaplus.usecases.users.LoadTutorMockDataUseCase;
import com.akademiaplus.usecases.tenant.LoadTenantSubscriptionMockDataUseCase;
import com.akademiaplus.usecases.tenant.LoadTenantBillingCycleMockDataUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Maps each {@link MockEntityType} to its loader, cleanup, and post-load hook beans.
 *
 * <p>The orchestrator consumes these maps to drive FK-safe execution
 * without hard-coding entity ordering.</p>
 *
 * <p>Three bean maps are produced:</p>
 * <ul>
 *   <li>{@code mockDataLoaders} — {@link IntConsumer} accepting the record count;
 *       one entry per {@linkplain MockEntityType#isLoadable() loadable} entity.</li>
 *   <li>{@code mockDataCleaners} — {@link Runnable} performing the cleanup;
 *       one entry per {@linkplain MockEntityType#isCleanable() cleanable} entity.</li>
 *   <li>{@code mockDataPostLoadHooks} — {@link MockDataPostLoadHook} for wiring
 *       generated IDs into downstream factories.</li>
 * </ul>
 */
@Configuration
public class MockDataRegistry {

    @Bean
    public Map<MockEntityType, IntConsumer> mockDataLoaders(
            LoadTenantMockDataUseCase tenantUseCase,
            LoadTenantSubscriptionMockDataUseCase tenantSubscriptionUseCase,
            LoadTenantBillingCycleMockDataUseCase tenantBillingCycleUseCase,
            LoadEmployeeMockDataUseCase employeeUseCase,
            LoadCollaboratorMockDataUseCase collaboratorUseCase,
            LoadAdultStudentMockDataUseCase adultStudentUseCase,
            LoadTutorMockDataUseCase tutorUseCase,
            LoadMinorStudentMockDataUseCase minorStudentUseCase,
            LoadCourseMockDataUseCase courseUseCase,
            LoadScheduleMockDataUseCase scheduleUseCase,
            LoadCourseEventMockDataUseCase courseEventUseCase,
            LoadCompensationMockDataUseCase compensationUseCase,
            LoadMembershipMockDataUseCase membershipUseCase,
            LoadMembershipAdultStudentMockDataUseCase membershipAdultStudentUseCase,
            LoadMembershipTutorMockDataUseCase membershipTutorUseCase,
            LoadPaymentAdultStudentMockDataUseCase paymentAdultStudentUseCase,
            LoadPaymentTutorMockDataUseCase paymentTutorUseCase,
            LoadCardPaymentInfoMockDataUseCase cardPaymentInfoUseCase,
            LoadStoreProductMockDataUseCase storeProductUseCase,
            LoadStoreTransactionMockDataUseCase storeTransactionUseCase,
            LoadStoreSaleItemMockDataUseCase storeSaleItemUseCase,
            LoadNotificationMockDataUseCase notificationUseCase,
            LoadNotificationDeliveryMockDataUseCase notificationDeliveryUseCase,
            LoadEmailMockDataUseCase emailUseCase,
            LoadEmailRecipientMockDataUseCase emailRecipientUseCase,
            LoadEmailAttachmentMockDataUseCase emailAttachmentUseCase,
            LoadAttendanceSessionMockDataUseCase attendanceSessionUseCase,
            LoadAttendanceRecordMockDataUseCase attendanceRecordUseCase,
            LoadDemoRequestMockDataUseCase demoRequestUseCase,
            LoadNotificationReadStatusMockDataUseCase notificationReadStatusUseCase,
            LoadPushDeviceMockDataUseCase pushDeviceUseCase,
            LoadCourseAvailableCollaboratorMockDataUseCase courseAvailableCollaboratorUseCase,
            LoadAdultStudentCourseMockDataUseCase adultStudentCourseUseCase,
            LoadMinorStudentCourseMockDataUseCase minorStudentCourseUseCase,
            LoadCourseEventAdultStudentAttendeeMockDataUseCase courseEventAdultStudentAttendeeUseCase,
            LoadCourseEventMinorStudentAttendeeMockDataUseCase courseEventMinorStudentAttendeeUseCase,
            LoadMembershipCourseMockDataUseCase membershipCourseUseCase,
            LoadCompensationCollaboratorMockDataUseCase compensationCollaboratorUseCase,
            LoadTenantBrandingMockDataUseCase tenantBrandingUseCase,
            LoadNewsFeedItemMockDataUseCase newsFeedItemUseCase,
            LoadTaskMockDataUseCase taskUseCase,
            LoadEmailTemplateMockDataUseCase emailTemplateUseCase,
            LoadEmailTemplateVariableMockDataUseCase emailTemplateVariableUseCase) {

        Map<MockEntityType, IntConsumer> loaders = new EnumMap<>(MockEntityType.class);

        // Tenant domain
        loaders.put(MockEntityType.TENANT, tenantUseCase::load);
        loaders.put(MockEntityType.TENANT_SUBSCRIPTION, tenantSubscriptionUseCase::load);
        loaders.put(MockEntityType.TENANT_BILLING_CYCLE, tenantBillingCycleUseCase::load);

        // People domain
        loaders.put(MockEntityType.EMPLOYEE, employeeUseCase::load);
        loaders.put(MockEntityType.COLLABORATOR, collaboratorUseCase::load);
        loaders.put(MockEntityType.ADULT_STUDENT, adultStudentUseCase::load);
        loaders.put(MockEntityType.TUTOR, tutorUseCase::load);
        loaders.put(MockEntityType.MINOR_STUDENT, minorStudentUseCase::load);

        // Course domain
        loaders.put(MockEntityType.COURSE, courseUseCase::load);
        loaders.put(MockEntityType.SCHEDULE, scheduleUseCase::load);
        loaders.put(MockEntityType.COURSE_EVENT, courseEventUseCase::load);

        // Billing domain
        loaders.put(MockEntityType.COMPENSATION, compensationUseCase::load);
        loaders.put(MockEntityType.MEMBERSHIP, membershipUseCase::load);
        loaders.put(MockEntityType.MEMBERSHIP_ADULT_STUDENT, membershipAdultStudentUseCase::load);
        loaders.put(MockEntityType.MEMBERSHIP_TUTOR, membershipTutorUseCase::load);
        loaders.put(MockEntityType.PAYMENT_ADULT_STUDENT, paymentAdultStudentUseCase::load);
        loaders.put(MockEntityType.PAYMENT_TUTOR, paymentTutorUseCase::load);
        loaders.put(MockEntityType.CARD_PAYMENT_INFO, cardPaymentInfoUseCase::load);

        // POS domain
        loaders.put(MockEntityType.STORE_PRODUCT, storeProductUseCase::load);
        loaders.put(MockEntityType.STORE_TRANSACTION, storeTransactionUseCase::load);
        loaders.put(MockEntityType.STORE_SALE_ITEM, storeSaleItemUseCase::load);

        // Notification domain
        loaders.put(MockEntityType.NOTIFICATION, notificationUseCase::load);
        loaders.put(MockEntityType.NOTIFICATION_DELIVERY, notificationDeliveryUseCase::load);

        // Email domain
        loaders.put(MockEntityType.EMAIL, emailUseCase::load);
        loaders.put(MockEntityType.EMAIL_RECIPIENT, emailRecipientUseCase::load);
        loaders.put(MockEntityType.EMAIL_ATTACHMENT, emailAttachmentUseCase::load);

        // Attendance domain
        loaders.put(MockEntityType.ATTENDANCE_SESSION, attendanceSessionUseCase::load);
        loaders.put(MockEntityType.ATTENDANCE_RECORD, attendanceRecordUseCase::load);

        // Lead management domain (platform-level)
        loaders.put(MockEntityType.DEMO_REQUEST, demoRequestUseCase::load);

        // Notification extensions (platform-level)
        loaders.put(MockEntityType.NOTIFICATION_READ_STATUS, notificationReadStatusUseCase::load);
        loaders.put(MockEntityType.PUSH_DEVICE, pushDeviceUseCase::load);

        // Bridge tables: course-people associations
        loaders.put(MockEntityType.COURSE_AVAILABLE_COLLABORATOR, courseAvailableCollaboratorUseCase::load);
        loaders.put(MockEntityType.ADULT_STUDENT_COURSE, adultStudentCourseUseCase::load);
        loaders.put(MockEntityType.MINOR_STUDENT_COURSE, minorStudentCourseUseCase::load);
        loaders.put(MockEntityType.COURSE_EVENT_ADULT_STUDENT_ATTENDEE, courseEventAdultStudentAttendeeUseCase::load);
        loaders.put(MockEntityType.COURSE_EVENT_MINOR_STUDENT_ATTENDEE, courseEventMinorStudentAttendeeUseCase::load);
        loaders.put(MockEntityType.MEMBERSHIP_COURSE, membershipCourseUseCase::load);
        loaders.put(MockEntityType.COMPENSATION_COLLABORATOR, compensationCollaboratorUseCase::load);

        // Entity tables: tenant config + content
        loaders.put(MockEntityType.TENANT_BRANDING, tenantBrandingUseCase::load);
        loaders.put(MockEntityType.NEWS_FEED_ITEM, newsFeedItemUseCase::load);
        loaders.put(MockEntityType.TASK, taskUseCase::load);

        // Config tables: email templates
        loaders.put(MockEntityType.EMAIL_TEMPLATE, emailTemplateUseCase::load);
        loaders.put(MockEntityType.EMAIL_TEMPLATE_VARIABLE, emailTemplateVariableUseCase::load);

        return Collections.unmodifiableMap(loaders);
    }

    @Bean
    public Map<MockEntityType, Runnable> mockDataCleaners(
            LoadTenantMockDataUseCase tenantUseCase,
            LoadTenantSubscriptionMockDataUseCase tenantSubscriptionUseCase,
            LoadTenantBillingCycleMockDataUseCase tenantBillingCycleUseCase,
            LoadEmployeeMockDataUseCase employeeUseCase,
            LoadCollaboratorMockDataUseCase collaboratorUseCase,
            LoadAdultStudentMockDataUseCase adultStudentUseCase,
            LoadTutorMockDataUseCase tutorUseCase,
            LoadMinorStudentMockDataUseCase minorStudentUseCase,
            LoadCourseMockDataUseCase courseUseCase,
            LoadScheduleMockDataUseCase scheduleUseCase,
            LoadCourseEventMockDataUseCase courseEventUseCase,
            LoadCompensationMockDataUseCase compensationUseCase,
            LoadMembershipMockDataUseCase membershipUseCase,
            LoadMembershipAdultStudentMockDataUseCase membershipAdultStudentUseCase,
            LoadMembershipTutorMockDataUseCase membershipTutorUseCase,
            LoadPaymentAdultStudentMockDataUseCase paymentAdultStudentUseCase,
            LoadPaymentTutorMockDataUseCase paymentTutorUseCase,
            LoadCardPaymentInfoMockDataUseCase cardPaymentInfoUseCase,
            LoadStoreProductMockDataUseCase storeProductUseCase,
            LoadStoreTransactionMockDataUseCase storeTransactionUseCase,
            LoadStoreSaleItemMockDataUseCase storeSaleItemUseCase,
            LoadNotificationMockDataUseCase notificationUseCase,
            LoadNotificationDeliveryMockDataUseCase notificationDeliveryUseCase,
            LoadEmailMockDataUseCase emailUseCase,
            LoadEmailRecipientMockDataUseCase emailRecipientUseCase,
            LoadEmailAttachmentMockDataUseCase emailAttachmentUseCase,
            @Qualifier("tenantSequenceDataCleanUp")
            DataCleanUp<TenantSequence, TenantSequence.TenantSequenceId> tenantSequenceCleanUp,
            @Qualifier("internalAuthDataCleanUp")
            DataCleanUp<InternalAuthDataModel, InternalAuthDataModel.InternalAuthCompositeId> internalAuthCleanUp,
            @Qualifier("customerAuthDataCleanUp")
            DataCleanUp<CustomerAuthDataModel, CustomerAuthDataModel.CustomerAuthCompositeId> customerAuthCleanUp,
            @Qualifier("personPIIDataCleanUp")
            DataCleanUp<PersonPIIDataModel, PersonPIIDataModel.PersonPIICompositeId> personPIICleanUp,
            LoadAttendanceSessionMockDataUseCase attendanceSessionUseCase,
            LoadAttendanceRecordMockDataUseCase attendanceRecordUseCase,
            LoadDemoRequestMockDataUseCase demoRequestUseCase,
            LoadNotificationReadStatusMockDataUseCase notificationReadStatusUseCase,
            LoadPushDeviceMockDataUseCase pushDeviceUseCase,
            LoadCourseAvailableCollaboratorMockDataUseCase courseAvailableCollaboratorUseCase,
            LoadAdultStudentCourseMockDataUseCase adultStudentCourseUseCase,
            LoadMinorStudentCourseMockDataUseCase minorStudentCourseUseCase,
            LoadCourseEventAdultStudentAttendeeMockDataUseCase courseEventAdultStudentAttendeeUseCase,
            LoadCourseEventMinorStudentAttendeeMockDataUseCase courseEventMinorStudentAttendeeUseCase,
            LoadMembershipCourseMockDataUseCase membershipCourseUseCase,
            LoadCompensationCollaboratorMockDataUseCase compensationCollaboratorUseCase,
            LoadTenantBrandingMockDataUseCase tenantBrandingUseCase,
            LoadNewsFeedItemMockDataUseCase newsFeedItemUseCase,
            LoadTaskMockDataUseCase taskUseCase,
            LoadEmailTemplateMockDataUseCase emailTemplateUseCase,
            LoadEmailTemplateVariableMockDataUseCase emailTemplateVariableUseCase) {

        Map<MockEntityType, Runnable> cleaners = new EnumMap<>(MockEntityType.class);

        // Tenant domain
        cleaners.put(MockEntityType.TENANT, tenantUseCase::clean);
        cleaners.put(MockEntityType.TENANT_SEQUENCE, tenantSequenceCleanUp::clean);
        cleaners.put(MockEntityType.TENANT_SUBSCRIPTION, tenantSubscriptionUseCase::clean);
        cleaners.put(MockEntityType.TENANT_BILLING_CYCLE, tenantBillingCycleUseCase::clean);

        // Shared cleanup-only tables
        cleaners.put(MockEntityType.PERSON_PII, personPIICleanUp::clean);
        cleaners.put(MockEntityType.INTERNAL_AUTH, internalAuthCleanUp::clean);
        cleaners.put(MockEntityType.CUSTOMER_AUTH, customerAuthCleanUp::clean);

        // People domain
        cleaners.put(MockEntityType.EMPLOYEE, employeeUseCase::clean);
        cleaners.put(MockEntityType.COLLABORATOR, collaboratorUseCase::clean);
        cleaners.put(MockEntityType.ADULT_STUDENT, adultStudentUseCase::clean);
        cleaners.put(MockEntityType.TUTOR, tutorUseCase::clean);
        cleaners.put(MockEntityType.MINOR_STUDENT, minorStudentUseCase::clean);

        // Course domain
        cleaners.put(MockEntityType.COURSE, courseUseCase::clean);
        cleaners.put(MockEntityType.SCHEDULE, scheduleUseCase::clean);
        cleaners.put(MockEntityType.COURSE_EVENT, courseEventUseCase::clean);

        // Billing domain
        cleaners.put(MockEntityType.COMPENSATION, compensationUseCase::clean);
        cleaners.put(MockEntityType.MEMBERSHIP, membershipUseCase::clean);
        cleaners.put(MockEntityType.MEMBERSHIP_ADULT_STUDENT, membershipAdultStudentUseCase::clean);
        cleaners.put(MockEntityType.MEMBERSHIP_TUTOR, membershipTutorUseCase::clean);
        cleaners.put(MockEntityType.PAYMENT_ADULT_STUDENT, paymentAdultStudentUseCase::clean);
        cleaners.put(MockEntityType.PAYMENT_TUTOR, paymentTutorUseCase::clean);
        cleaners.put(MockEntityType.CARD_PAYMENT_INFO, cardPaymentInfoUseCase::clean);

        // POS domain
        cleaners.put(MockEntityType.STORE_PRODUCT, storeProductUseCase::clean);
        cleaners.put(MockEntityType.STORE_TRANSACTION, storeTransactionUseCase::clean);
        cleaners.put(MockEntityType.STORE_SALE_ITEM, storeSaleItemUseCase::clean);

        // Notification domain
        cleaners.put(MockEntityType.NOTIFICATION, notificationUseCase::clean);
        cleaners.put(MockEntityType.NOTIFICATION_DELIVERY, notificationDeliveryUseCase::clean);

        // Email domain
        cleaners.put(MockEntityType.EMAIL, emailUseCase::clean);
        cleaners.put(MockEntityType.EMAIL_RECIPIENT, emailRecipientUseCase::clean);
        cleaners.put(MockEntityType.EMAIL_ATTACHMENT, emailAttachmentUseCase::clean);

        // Attendance domain
        cleaners.put(MockEntityType.ATTENDANCE_SESSION, attendanceSessionUseCase::clean);
        cleaners.put(MockEntityType.ATTENDANCE_RECORD, attendanceRecordUseCase::clean);

        // Lead management domain (platform-level)
        cleaners.put(MockEntityType.DEMO_REQUEST, demoRequestUseCase::clean);

        // Notification extensions (platform-level)
        cleaners.put(MockEntityType.NOTIFICATION_READ_STATUS, notificationReadStatusUseCase::clean);
        cleaners.put(MockEntityType.PUSH_DEVICE, pushDeviceUseCase::clean);

        // Bridge tables
        cleaners.put(MockEntityType.COURSE_AVAILABLE_COLLABORATOR, courseAvailableCollaboratorUseCase::clean);
        cleaners.put(MockEntityType.ADULT_STUDENT_COURSE, adultStudentCourseUseCase::clean);
        cleaners.put(MockEntityType.MINOR_STUDENT_COURSE, minorStudentCourseUseCase::clean);
        cleaners.put(MockEntityType.COURSE_EVENT_ADULT_STUDENT_ATTENDEE, courseEventAdultStudentAttendeeUseCase::clean);
        cleaners.put(MockEntityType.COURSE_EVENT_MINOR_STUDENT_ATTENDEE, courseEventMinorStudentAttendeeUseCase::clean);
        cleaners.put(MockEntityType.MEMBERSHIP_COURSE, membershipCourseUseCase::clean);
        cleaners.put(MockEntityType.COMPENSATION_COLLABORATOR, compensationCollaboratorUseCase::clean);

        // Entity tables
        cleaners.put(MockEntityType.TENANT_BRANDING, tenantBrandingUseCase::clean);
        cleaners.put(MockEntityType.NEWS_FEED_ITEM, newsFeedItemUseCase::clean);
        cleaners.put(MockEntityType.TASK, taskUseCase::clean);

        // Config tables
        cleaners.put(MockEntityType.EMAIL_TEMPLATE, emailTemplateUseCase::clean);
        cleaners.put(MockEntityType.EMAIL_TEMPLATE_VARIABLE, emailTemplateVariableUseCase::clean);

        return Collections.unmodifiableMap(cleaners);
    }

    @Bean
    public Map<MockEntityType, MockDataPostLoadHook> mockDataPostLoadHooks(
            TenantRepository tenantRepository,
            TenantContextHolder tenantContextHolder,
            TutorRepository tutorRepository,
            MinorStudentFactory minorStudentFactory,
            CourseRepository courseRepository,
            ScheduleRepository scheduleRepository,
            CollaboratorRepository collaboratorRepository,
            ScheduleFactory scheduleFactory,
            CourseEventFactory courseEventFactory,
            MembershipRepository membershipRepository,
            AdultStudentRepository adultStudentRepository,
            MembershipAdultStudentFactory membershipAdultStudentFactory,
            MembershipTutorFactory membershipTutorFactory,
            MembershipAdultStudentRepository membershipAdultStudentRepository,
            MembershipTutorRepository membershipTutorRepository,
            PaymentAdultStudentFactory paymentAdultStudentFactory,
            PaymentTutorFactory paymentTutorFactory,
            PaymentAdultStudentRepository paymentAdultStudentRepository,
            CardPaymentInfoFactory cardPaymentInfoFactory,
            StoreTransactionRepository storeTransactionRepository,
            StoreProductRepository storeProductRepository,
            StoreSaleItemFactory storeSaleItemFactory,
            NotificationDeliveryFactory notificationDeliveryFactory,
            NotificationRepository notificationRepository,
            EmailRepository emailRepository,
            EmailRecipientFactory emailRecipientFactory,
            EmailAttachmentFactory emailAttachmentFactory,
            CourseEventRepository courseEventRepository,
            AttendanceSessionFactory attendanceSessionFactory,
            AttendanceSessionRepository attendanceSessionRepository,
            AttendanceRecordFactory attendanceRecordFactory,
            NotificationReadStatusFactory notificationReadStatusFactory,
            PushDeviceFactory pushDeviceFactory,
            CourseAvailableCollaboratorFactory courseAvailableCollaboratorFactory,
            AdultStudentCourseFactory adultStudentCourseFactory,
            MinorStudentCourseFactory minorStudentCourseFactory,
            CourseEventAdultStudentAttendeeFactory courseEventAdultStudentAttendeeFactory,
            CourseEventMinorStudentAttendeeFactory courseEventMinorStudentAttendeeFactory,
            MembershipCourseFactory membershipCourseFactory,
            CompensationCollaboratorFactory compensationCollaboratorFactory,
            NewsFeedItemFactory newsFeedItemFactory,
            TaskFactory taskFactory,
            EmailTemplateVariableFactory emailTemplateVariableFactory,
            EmployeeRepository employeeRepository,
            CompensationRepository compensationRepository,
            MinorStudentRepository minorStudentRepository,
            EmailTemplateRepository emailTemplateRepository) {

        Map<MockEntityType, MockDataPostLoadHook> hooks = new EnumMap<>(MockEntityType.class);

        // After TENANT: set TenantContextHolder with first tenant's ID
        hooks.put(MockEntityType.TENANT, () -> {
            Long firstTenantId = tenantRepository.findAll().stream()
                    .map(TenantDataModel::getTenantId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No tenants found after TENANT load"));
            tenantContextHolder.setTenantId(firstTenantId);
        });

        // After TUTOR: inject tutor IDs into MinorStudentFactory and MembershipTutorFactory
        hooks.put(MockEntityType.TUTOR, () -> {
            List<Long> tutorIds = tutorRepository.findAll().stream()
                    .map(TutorDataModel::getTutorId)
                    .toList();
            minorStudentFactory.setAvailableTutorIds(tutorIds);
            membershipTutorFactory.setAvailableTutorIds(tutorIds);
        });

        // After COURSE: inject course IDs into ScheduleFactory and downstream factories
        hooks.put(MockEntityType.COURSE, () -> {
            List<Long> courseIds = courseRepository.findAll().stream()
                    .map(CourseDataModel::getCourseId)
                    .toList();
            scheduleFactory.setAvailableCourseIds(courseIds);
            courseEventFactory.setAvailableCourseIds(courseIds);
            membershipAdultStudentFactory.setAvailableCourseIds(courseIds);
            membershipTutorFactory.setAvailableCourseIds(courseIds);
            courseAvailableCollaboratorFactory.setAvailableCourseIds(courseIds);
            adultStudentCourseFactory.setAvailableCourseIds(courseIds);
            minorStudentCourseFactory.setAvailableCourseIds(courseIds);
            membershipCourseFactory.setAvailableCourseIds(courseIds);
            newsFeedItemFactory.setAvailableCourseIds(courseIds);
        });

        // After SCHEDULE: inject schedule IDs into CourseEventFactory
        hooks.put(MockEntityType.SCHEDULE, () -> {
            List<Long> scheduleIds = scheduleRepository.findAll().stream()
                    .map(ScheduleDataModel::getScheduleId)
                    .toList();
            courseEventFactory.setAvailableScheduleIds(scheduleIds);
        });

        // After COLLABORATOR: inject collaborator IDs into downstream factories
        hooks.put(MockEntityType.COLLABORATOR, () -> {
            List<Long> collaboratorIds = collaboratorRepository.findAll().stream()
                    .map(CollaboratorDataModel::getCollaboratorId)
                    .toList();
            courseEventFactory.setAvailableCollaboratorIds(collaboratorIds);
            courseAvailableCollaboratorFactory.setAvailableCollaboratorIds(collaboratorIds);
            compensationCollaboratorFactory.setAvailableCollaboratorIds(collaboratorIds);
        });

        // After MEMBERSHIP: inject membership IDs into association factories
        hooks.put(MockEntityType.MEMBERSHIP, () -> {
            List<Long> membershipIds = membershipRepository.findAll().stream()
                    .map(MembershipDataModel::getMembershipId)
                    .toList();
            membershipAdultStudentFactory.setAvailableMembershipIds(membershipIds);
            membershipTutorFactory.setAvailableMembershipIds(membershipIds);
            membershipCourseFactory.setAvailableMembershipIds(membershipIds);
        });

        // After ADULT_STUDENT: inject adult student IDs into downstream factories
        hooks.put(MockEntityType.ADULT_STUDENT, () -> {
            List<Long> adultStudentIds = adultStudentRepository.findAll().stream()
                    .map(AdultStudentDataModel::getAdultStudentId)
                    .toList();
            membershipAdultStudentFactory.setAvailableAdultStudentIds(adultStudentIds);
            attendanceRecordFactory.setAvailableAdultStudentIds(adultStudentIds);
            pushDeviceFactory.setAvailableUserIds(adultStudentIds);
            notificationReadStatusFactory.setAvailableUserIds(adultStudentIds);
            adultStudentCourseFactory.setAvailableAdultStudentIds(adultStudentIds);
            courseEventAdultStudentAttendeeFactory.setAvailableAdultStudentIds(adultStudentIds);
        });

        // After MEMBERSHIP_ADULT_STUDENT: inject IDs into PaymentAdultStudentFactory
        hooks.put(MockEntityType.MEMBERSHIP_ADULT_STUDENT, () -> {
            List<Long> membershipAdultStudentIds = membershipAdultStudentRepository.findAll().stream()
                    .map(MembershipAdultStudentDataModel::getMembershipAdultStudentId)
                    .toList();
            paymentAdultStudentFactory.setAvailableMembershipAdultStudentIds(membershipAdultStudentIds);
        });

        // After MEMBERSHIP_TUTOR: inject IDs into PaymentTutorFactory
        hooks.put(MockEntityType.MEMBERSHIP_TUTOR, () -> {
            List<Long> membershipTutorIds = membershipTutorRepository.findAll().stream()
                    .map(MembershipTutorDataModel::getMembershipTutorId)
                    .toList();
            paymentTutorFactory.setAvailableMembershipTutorIds(membershipTutorIds);
        });

        // After PAYMENT_ADULT_STUDENT: inject payment IDs into CardPaymentInfoFactory
        hooks.put(MockEntityType.PAYMENT_ADULT_STUDENT, () -> {
            List<Long> paymentIds = paymentAdultStudentRepository.findAll().stream()
                    .map(PaymentAdultStudentDataModel::getPaymentAdultStudentId)
                    .toList();
            cardPaymentInfoFactory.setAvailablePaymentAdultStudentIds(paymentIds);
        });

        // After STORE_TRANSACTION: inject transaction IDs into StoreSaleItemFactory
        hooks.put(MockEntityType.STORE_TRANSACTION, () -> {
            List<Long> transactionIds = storeTransactionRepository.findAll().stream()
                    .map(StoreTransactionDataModel::getStoreTransactionId)
                    .toList();
            storeSaleItemFactory.setAvailableStoreTransactionIds(transactionIds);
        });

        // After STORE_PRODUCT: inject product IDs into StoreSaleItemFactory
        hooks.put(MockEntityType.STORE_PRODUCT, () -> {
            List<Long> productIds = storeProductRepository.findAll().stream()
                    .map(StoreProductDataModel::getStoreProductId)
                    .toList();
            storeSaleItemFactory.setAvailableStoreProductIds(productIds);
        });

        // After NOTIFICATION: inject notification IDs into downstream factories
        hooks.put(MockEntityType.NOTIFICATION, () -> {
            List<Long> notificationIds = notificationRepository.findAll().stream()
                    .map(NotificationDataModel::getNotificationId)
                    .toList();
            notificationDeliveryFactory.setAvailableNotificationIds(notificationIds);
            notificationReadStatusFactory.setAvailableNotificationIds(notificationIds);
        });

        // After EMAIL: inject email IDs into EmailRecipientFactory and EmailAttachmentFactory
        hooks.put(MockEntityType.EMAIL, () -> {
            List<Long> emailIds = emailRepository.findAll().stream()
                    .map(EmailDataModel::getEmailId)
                    .toList();
            emailRecipientFactory.setAvailableEmailIds(emailIds);
            emailAttachmentFactory.setAvailableEmailIds(emailIds);
        });

        // After COURSE_EVENT: inject course event IDs into downstream factories
        hooks.put(MockEntityType.COURSE_EVENT, () -> {
            List<Long> courseEventIds = courseEventRepository.findAll().stream()
                    .map(CourseEventDataModel::getCourseEventId)
                    .toList();
            attendanceSessionFactory.setAvailableCourseEventIds(courseEventIds);
            courseEventAdultStudentAttendeeFactory.setAvailableCourseEventIds(courseEventIds);
            courseEventMinorStudentAttendeeFactory.setAvailableCourseEventIds(courseEventIds);
        });

        // After ATTENDANCE_SESSION: inject session IDs into AttendanceRecordFactory
        hooks.put(MockEntityType.ATTENDANCE_SESSION, () -> {
            List<Long> sessionIds = attendanceSessionRepository.findAll().stream()
                    .map(AttendanceSessionDataModel::getAttendanceSessionId)
                    .toList();
            attendanceRecordFactory.setAvailableAttendanceSessionIds(sessionIds);
        });

        // After MINOR_STUDENT: inject minor student IDs into downstream factories
        hooks.put(MockEntityType.MINOR_STUDENT, () -> {
            List<Long> minorStudentIds = minorStudentRepository.findAll().stream()
                    .map(MinorStudentDataModel::getMinorStudentId)
                    .toList();
            minorStudentCourseFactory.setAvailableMinorStudentIds(minorStudentIds);
            courseEventMinorStudentAttendeeFactory.setAvailableMinorStudentIds(minorStudentIds);
        });

        // After EMPLOYEE: inject employee IDs into NewsFeedItemFactory and TaskFactory
        hooks.put(MockEntityType.EMPLOYEE, () -> {
            List<Long> employeeIds = employeeRepository.findAll().stream()
                    .map(EmployeeDataModel::getEmployeeId)
                    .toList();
            newsFeedItemFactory.setAvailableEmployeeIds(employeeIds);
            taskFactory.setAvailableEmployeeIds(employeeIds);
        });

        // After COMPENSATION: inject compensation IDs into CompensationCollaboratorFactory
        hooks.put(MockEntityType.COMPENSATION, () -> {
            List<Long> compensationIds = compensationRepository.findAll().stream()
                    .map(CompensationDataModel::getCompensationId)
                    .toList();
            compensationCollaboratorFactory.setAvailableCompensationIds(compensationIds);
        });

        // After EMAIL_TEMPLATE: inject template IDs into EmailTemplateVariableFactory
        hooks.put(MockEntityType.EMAIL_TEMPLATE, () -> {
            List<Long> templateIds = emailTemplateRepository.findAll().stream()
                    .map(EmailTemplateDataModel::getTemplateId)
                    .toList();
            emailTemplateVariableFactory.setAvailableTemplateIds(templateIds);
        });

        return Collections.unmodifiableMap(hooks);
    }
}
