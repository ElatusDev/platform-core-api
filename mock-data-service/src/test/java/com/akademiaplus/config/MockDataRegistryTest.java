/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.attendance.AttendanceSessionDataModel;
import com.akademiaplus.attendance.interfaceadapters.AttendanceSessionRepository;
import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.billing.payroll.CompensationDataModel;
import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import com.akademiaplus.notification.interfaceadapters.EmailRepository;
import com.akademiaplus.notification.interfaceadapters.EmailTemplateRepository;
import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.email.EmailDataModel;
import com.akademiaplus.notifications.email.EmailTemplateDataModel;
import com.akademiaplus.payroll.interfaceadapters.CompensationRepository;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import com.akademiaplus.store.interfaceadapters.StoreTransactionRepository;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.usecases.attendance.LoadAttendanceRecordMockDataUseCase;
import com.akademiaplus.usecases.attendance.LoadAttendanceSessionMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadCardPaymentInfoMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadCompensationCollaboratorMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadCompensationMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadMembershipAdultStudentMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadMembershipCourseMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadMembershipMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadMembershipTutorMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadPaymentAdultStudentMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadPaymentTutorMockDataUseCase;
import com.akademiaplus.usecases.course.LoadAdultStudentCourseMockDataUseCase;
import com.akademiaplus.usecases.course.LoadCourseAvailableCollaboratorMockDataUseCase;
import com.akademiaplus.usecases.course.LoadCourseEventAdultStudentAttendeeMockDataUseCase;
import com.akademiaplus.usecases.course.LoadCourseEventMinorStudentAttendeeMockDataUseCase;
import com.akademiaplus.usecases.course.LoadCourseEventMockDataUseCase;
import com.akademiaplus.usecases.course.LoadCourseMockDataUseCase;
import com.akademiaplus.usecases.course.LoadMinorStudentCourseMockDataUseCase;
import com.akademiaplus.usecases.course.LoadScheduleMockDataUseCase;
import com.akademiaplus.usecases.leadmanagement.LoadDemoRequestMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailAttachmentMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailRecipientMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailTemplateMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailTemplateVariableMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadNewsFeedItemMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadNotificationDeliveryMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadNotificationMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadNotificationReadStatusMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadPushDeviceMockDataUseCase;
import com.akademiaplus.usecases.store.LoadStoreSaleItemMockDataUseCase;
import com.akademiaplus.usecases.store.LoadStoreProductMockDataUseCase;
import com.akademiaplus.usecases.store.LoadStoreTransactionMockDataUseCase;
import com.akademiaplus.usecases.task.LoadTaskMockDataUseCase;
import com.akademiaplus.usecases.tenant.LoadTenantBillingCycleMockDataUseCase;
import com.akademiaplus.usecases.tenant.LoadTenantBrandingMockDataUseCase;
import com.akademiaplus.usecases.tenant.LoadTenantSubscriptionMockDataUseCase;
import com.akademiaplus.usecases.users.LoadAdultStudentMockDataUseCase;
import com.akademiaplus.usecases.users.LoadCollaboratorMockDataUseCase;
import com.akademiaplus.usecases.users.LoadEmployeeMockDataUseCase;
import com.akademiaplus.usecases.users.LoadMinorStudentMockDataUseCase;
import com.akademiaplus.usecases.users.LoadTenantMockDataUseCase;
import com.akademiaplus.usecases.users.LoadTutorMockDataUseCase;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.mock.attendance.AttendanceRecordFactory;
import com.akademiaplus.util.mock.attendance.AttendanceSessionFactory;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import static com.akademiaplus.config.MockEntityType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@DisplayName("MockDataRegistry")
@ExtendWith(MockitoExtension.class)
class MockDataRegistryTest {

    private static final int RECORD_COUNT = 5;
    private static final long TENANT_ID = 1L;
    private static final long TUTOR_ID_ONE = 10L;
    private static final long TUTOR_ID_TWO = 20L;
    private static final long COURSE_ID_ONE = 100L;
    private static final long SCHEDULE_ID_ONE = 200L;
    private static final long COLLABORATOR_ID_ONE = 300L;
    private static final long MEMBERSHIP_ID_ONE = 400L;
    private static final long ADULT_STUDENT_ID_ONE = 500L;
    private static final long MEMBERSHIP_AS_ID_ONE = 600L;
    private static final long MEMBERSHIP_TUTOR_ID_ONE = 700L;
    private static final long PAYMENT_AS_ID_ONE = 800L;
    private static final long STORE_TRANSACTION_ID_ONE = 900L;
    private static final long STORE_PRODUCT_ID_ONE = 1000L;
    private static final long NOTIFICATION_ID_ONE = 1100L;
    private static final long EMAIL_ID_ONE = 1200L;
    private static final long MINOR_STUDENT_ID_ONE = 1300L;
    private static final long EMPLOYEE_ID_ONE = 1400L;
    private static final long COMPENSATION_ID_ONE = 1500L;
    private static final long COURSE_EVENT_ID_ONE = 1600L;
    private static final long EMAIL_TEMPLATE_ID_ONE = 1700L;

    // ── Users domain ──
    @Mock private LoadTenantMockDataUseCase tenantUseCase;
    @Mock private LoadEmployeeMockDataUseCase employeeUseCase;
    @Mock private LoadCollaboratorMockDataUseCase collaboratorUseCase;
    @Mock private LoadAdultStudentMockDataUseCase adultStudentUseCase;
    @Mock private LoadTutorMockDataUseCase tutorUseCase;
    @Mock private LoadMinorStudentMockDataUseCase minorStudentUseCase;

    // ── Tenant domain ──
    @Mock private LoadTenantSubscriptionMockDataUseCase tenantSubscriptionUseCase;
    @Mock private LoadTenantBillingCycleMockDataUseCase tenantBillingCycleUseCase;
    @Mock private LoadTenantBrandingMockDataUseCase tenantBrandingUseCase;

    // ── Course domain ──
    @Mock private LoadCourseMockDataUseCase courseUseCase;
    @Mock private LoadScheduleMockDataUseCase scheduleUseCase;
    @Mock private LoadCourseEventMockDataUseCase courseEventUseCase;

    // ── Billing domain ──
    @Mock private LoadCompensationMockDataUseCase compensationUseCase;
    @Mock private LoadMembershipMockDataUseCase membershipUseCase;
    @Mock private LoadMembershipAdultStudentMockDataUseCase membershipAdultStudentUseCase;
    @Mock private LoadMembershipTutorMockDataUseCase membershipTutorUseCase;
    @Mock private LoadPaymentAdultStudentMockDataUseCase paymentAdultStudentUseCase;
    @Mock private LoadPaymentTutorMockDataUseCase paymentTutorUseCase;
    @Mock private LoadCardPaymentInfoMockDataUseCase cardPaymentInfoUseCase;

    // ── POS domain ──
    @Mock private LoadStoreProductMockDataUseCase storeProductUseCase;
    @Mock private LoadStoreTransactionMockDataUseCase storeTransactionUseCase;
    @Mock private LoadStoreSaleItemMockDataUseCase storeSaleItemUseCase;

    // ── Notification domain ──
    @Mock private LoadNotificationMockDataUseCase notificationUseCase;
    @Mock private LoadNotificationDeliveryMockDataUseCase notificationDeliveryUseCase;

    // ── Email domain ──
    @Mock private LoadEmailMockDataUseCase emailUseCase;
    @Mock private LoadEmailRecipientMockDataUseCase emailRecipientUseCase;
    @Mock private LoadEmailAttachmentMockDataUseCase emailAttachmentUseCase;

    // ── Attendance domain ──
    @Mock private LoadAttendanceSessionMockDataUseCase attendanceSessionUseCase;
    @Mock private LoadAttendanceRecordMockDataUseCase attendanceRecordUseCase;

    // ── Lead management domain ──
    @Mock private LoadDemoRequestMockDataUseCase demoRequestUseCase;

    // ── Notification extensions ──
    @Mock private LoadNotificationReadStatusMockDataUseCase notificationReadStatusUseCase;
    @Mock private LoadPushDeviceMockDataUseCase pushDeviceUseCase;

    // ── Bridge table use cases ──
    @Mock private LoadCourseAvailableCollaboratorMockDataUseCase courseAvailableCollaboratorUseCase;
    @Mock private LoadAdultStudentCourseMockDataUseCase adultStudentCourseUseCase;
    @Mock private LoadMinorStudentCourseMockDataUseCase minorStudentCourseUseCase;
    @Mock private LoadCourseEventAdultStudentAttendeeMockDataUseCase courseEventAdultStudentAttendeeUseCase;
    @Mock private LoadCourseEventMinorStudentAttendeeMockDataUseCase courseEventMinorStudentAttendeeUseCase;
    @Mock private LoadMembershipCourseMockDataUseCase membershipCourseUseCase;
    @Mock private LoadCompensationCollaboratorMockDataUseCase compensationCollaboratorUseCase;

    // ── Entity/config table use cases ──
    @Mock private LoadNewsFeedItemMockDataUseCase newsFeedItemUseCase;
    @Mock private LoadTaskMockDataUseCase taskUseCase;
    @Mock private LoadEmailTemplateMockDataUseCase emailTemplateUseCase;
    @Mock private LoadEmailTemplateVariableMockDataUseCase emailTemplateVariableUseCase;

    // ── Cleanup-only beans ──
    @Mock private DataCleanUp<TenantSequence, TenantSequence.TenantSequenceId> tenantSequenceCleanUp;
    @Mock private DataCleanUp<InternalAuthDataModel, InternalAuthDataModel.InternalAuthCompositeId> internalAuthCleanUp;
    @Mock private DataCleanUp<CustomerAuthDataModel, CustomerAuthDataModel.CustomerAuthCompositeId> customerAuthCleanUp;
    @Mock private DataCleanUp<PersonPIIDataModel, PersonPIIDataModel.PersonPIICompositeId> personPIICleanUp;

    // ── Repositories for hooks ──
    @Mock private TenantRepository tenantRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private AdultStudentRepository adultStudentRepository;
    @Mock private MembershipAdultStudentRepository membershipAdultStudentRepository;
    @Mock private MembershipTutorRepository membershipTutorRepository;
    @Mock private PaymentAdultStudentRepository paymentAdultStudentRepository;
    @Mock private StoreTransactionRepository storeTransactionRepository;
    @Mock private StoreProductRepository storeProductRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private EmailRepository emailRepository;
    @Mock private CourseEventRepository courseEventRepository;
    @Mock private AttendanceSessionRepository attendanceSessionRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private CompensationRepository compensationRepository;
    @Mock private MinorStudentRepository minorStudentRepository;
    @Mock private EmailTemplateRepository emailTemplateRepository;

    // ── Factories for hooks ──
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private MinorStudentFactory minorStudentFactory;
    @Mock private ScheduleFactory scheduleFactory;
    @Mock private CourseEventFactory courseEventFactory;
    @Mock private MembershipAdultStudentFactory membershipAdultStudentFactory;
    @Mock private MembershipTutorFactory membershipTutorFactory;
    @Mock private PaymentAdultStudentFactory paymentAdultStudentFactory;
    @Mock private PaymentTutorFactory paymentTutorFactory;
    @Mock private CardPaymentInfoFactory cardPaymentInfoFactory;
    @Mock private StoreSaleItemFactory storeSaleItemFactory;
    @Mock private NotificationDeliveryFactory notificationDeliveryFactory;
    @Mock private EmailRecipientFactory emailRecipientFactory;
    @Mock private EmailAttachmentFactory emailAttachmentFactory;
    @Mock private AttendanceSessionFactory attendanceSessionFactory;
    @Mock private AttendanceRecordFactory attendanceRecordFactory;
    @Mock private NotificationReadStatusFactory notificationReadStatusFactory;
    @Mock private PushDeviceFactory pushDeviceFactory;
    @Mock private CourseAvailableCollaboratorFactory courseAvailableCollaboratorFactory;
    @Mock private AdultStudentCourseFactory adultStudentCourseFactory;
    @Mock private MinorStudentCourseFactory minorStudentCourseFactory;
    @Mock private CourseEventAdultStudentAttendeeFactory courseEventAdultStudentAttendeeFactory;
    @Mock private CourseEventMinorStudentAttendeeFactory courseEventMinorStudentAttendeeFactory;
    @Mock private MembershipCourseFactory membershipCourseFactory;
    @Mock private CompensationCollaboratorFactory compensationCollaboratorFactory;
    @Mock private NewsFeedItemFactory newsFeedItemFactory;
    @Mock private TaskFactory taskFactory;
    @Mock private EmailTemplateVariableFactory emailTemplateVariableFactory;

    private MockDataRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MockDataRegistry();
    }

    @Nested
    @DisplayName("mockDataLoaders bean")
    class MockDataLoaders {

        private Map<MockEntityType, IntConsumer> loaders;

        @BeforeEach
        void setUp() {
            loaders = registry.mockDataLoaders(
                    tenantUseCase, tenantSubscriptionUseCase, tenantBillingCycleUseCase,
                    employeeUseCase, collaboratorUseCase, adultStudentUseCase,
                    tutorUseCase, minorStudentUseCase,
                    courseUseCase, scheduleUseCase, courseEventUseCase,
                    compensationUseCase, membershipUseCase,
                    membershipAdultStudentUseCase, membershipTutorUseCase,
                    paymentAdultStudentUseCase, paymentTutorUseCase,
                    cardPaymentInfoUseCase,
                    storeProductUseCase, storeTransactionUseCase,
                    storeSaleItemUseCase,
                    notificationUseCase, notificationDeliveryUseCase,
                    emailUseCase, emailRecipientUseCase, emailAttachmentUseCase,
                    attendanceSessionUseCase, attendanceRecordUseCase,
                    demoRequestUseCase,
                    notificationReadStatusUseCase, pushDeviceUseCase,
                    courseAvailableCollaboratorUseCase, adultStudentCourseUseCase,
                    minorStudentCourseUseCase,
                    courseEventAdultStudentAttendeeUseCase, courseEventMinorStudentAttendeeUseCase,
                    membershipCourseUseCase, compensationCollaboratorUseCase,
                    tenantBrandingUseCase, newsFeedItemUseCase, taskUseCase,
                    emailTemplateUseCase, emailTemplateVariableUseCase);
        }

        @Test
        @DisplayName("Should contain all loadable entity types")
        void shouldContainAllLoadableEntityTypes() {
            // Given — loaders built in setUp

            // When & Then
            assertThat(loaders).containsOnlyKeys(
                    TENANT, TENANT_SUBSCRIPTION, TENANT_BILLING_CYCLE,
                    EMPLOYEE, COLLABORATOR, ADULT_STUDENT, TUTOR, MINOR_STUDENT,
                    COURSE, SCHEDULE, COURSE_EVENT,
                    COMPENSATION, MEMBERSHIP, MEMBERSHIP_ADULT_STUDENT, MEMBERSHIP_TUTOR,
                    PAYMENT_ADULT_STUDENT, PAYMENT_TUTOR, CARD_PAYMENT_INFO,
                    STORE_PRODUCT, STORE_TRANSACTION, STORE_SALE_ITEM,
                    NOTIFICATION, NOTIFICATION_DELIVERY,
                    EMAIL, EMAIL_RECIPIENT, EMAIL_ATTACHMENT,
                    ATTENDANCE_SESSION, ATTENDANCE_RECORD,
                    DEMO_REQUEST,
                    NOTIFICATION_READ_STATUS, PUSH_DEVICE,
                    COURSE_AVAILABLE_COLLABORATOR, ADULT_STUDENT_COURSE, MINOR_STUDENT_COURSE,
                    COURSE_EVENT_ADULT_STUDENT_ATTENDEE, COURSE_EVENT_MINOR_STUDENT_ATTENDEE,
                    MEMBERSHIP_COURSE, COMPENSATION_COLLABORATOR,
                    TENANT_BRANDING, NEWS_FEED_ITEM, TASK,
                    EMAIL_TEMPLATE, EMAIL_TEMPLATE_VARIABLE);
        }

        @Test
        @DisplayName("Should delegate tenant loader to tenant use case")
        void shouldDelegateTenantLoader_toTenantUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(TENANT).accept(RECORD_COUNT);

            // Then — state assertion
            assertThat(loaders).containsKey(TENANT);

            // Then — interaction assertion
            InOrder inOrder = inOrder(tenantUseCase);
            inOrder.verify(tenantUseCase, times(1)).load(RECORD_COUNT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should delegate course loader to course use case")
        void shouldDelegateCourseLoader_toCourseUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(COURSE).accept(RECORD_COUNT);

            // Then — state assertion
            assertThat(loaders).containsKey(COURSE);

            // Then — interaction assertion
            InOrder inOrder = inOrder(courseUseCase);
            inOrder.verify(courseUseCase, times(1)).load(RECORD_COUNT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should delegate compensation loader to compensation use case")
        void shouldDelegateCompensationLoader_toCompensationUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(COMPENSATION).accept(RECORD_COUNT);

            // Then — state assertion
            assertThat(loaders).containsKey(COMPENSATION);

            // Then — interaction assertion
            InOrder inOrder = inOrder(compensationUseCase);
            inOrder.verify(compensationUseCase, times(1)).load(RECORD_COUNT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should delegate notification loader to notification use case")
        void shouldDelegateNotificationLoader_toNotificationUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(NOTIFICATION).accept(RECORD_COUNT);

            // Then — state assertion
            assertThat(loaders).containsKey(NOTIFICATION);

            // Then — interaction assertion
            InOrder inOrder = inOrder(notificationUseCase);
            inOrder.verify(notificationUseCase, times(1)).load(RECORD_COUNT);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return unmodifiable map")
        void shouldReturnUnmodifiableMap() {
            // Given — loaders built in setUp

            // When & Then
            assertThatThrownBy(() -> loaders.put(TENANT, count -> {}))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("mockDataCleaners bean")
    class MockDataCleaners {

        private Map<MockEntityType, Runnable> cleaners;

        @BeforeEach
        void setUp() {
            cleaners = registry.mockDataCleaners(
                    tenantUseCase, tenantSubscriptionUseCase, tenantBillingCycleUseCase,
                    employeeUseCase, collaboratorUseCase, adultStudentUseCase,
                    tutorUseCase, minorStudentUseCase,
                    courseUseCase, scheduleUseCase, courseEventUseCase,
                    compensationUseCase, membershipUseCase,
                    membershipAdultStudentUseCase, membershipTutorUseCase,
                    paymentAdultStudentUseCase, paymentTutorUseCase,
                    cardPaymentInfoUseCase,
                    storeProductUseCase, storeTransactionUseCase,
                    storeSaleItemUseCase,
                    notificationUseCase, notificationDeliveryUseCase,
                    emailUseCase, emailRecipientUseCase, emailAttachmentUseCase,
                    tenantSequenceCleanUp, internalAuthCleanUp,
                    customerAuthCleanUp, personPIICleanUp,
                    attendanceSessionUseCase, attendanceRecordUseCase,
                    demoRequestUseCase,
                    notificationReadStatusUseCase, pushDeviceUseCase,
                    courseAvailableCollaboratorUseCase, adultStudentCourseUseCase,
                    minorStudentCourseUseCase,
                    courseEventAdultStudentAttendeeUseCase, courseEventMinorStudentAttendeeUseCase,
                    membershipCourseUseCase, compensationCollaboratorUseCase,
                    tenantBrandingUseCase, newsFeedItemUseCase, taskUseCase,
                    emailTemplateUseCase, emailTemplateVariableUseCase);
        }

        @Test
        @DisplayName("Should contain all cleanable entity types")
        void shouldContainAllCleanableEntityTypes() {
            // Given — cleaners built in setUp

            // When & Then
            assertThat(cleaners).containsOnlyKeys(
                    TENANT, TENANT_SEQUENCE, TENANT_SUBSCRIPTION, TENANT_BILLING_CYCLE,
                    PERSON_PII, INTERNAL_AUTH, CUSTOMER_AUTH,
                    EMPLOYEE, COLLABORATOR, ADULT_STUDENT, TUTOR, MINOR_STUDENT,
                    COURSE, SCHEDULE, COURSE_EVENT,
                    COMPENSATION, MEMBERSHIP, MEMBERSHIP_ADULT_STUDENT, MEMBERSHIP_TUTOR,
                    PAYMENT_ADULT_STUDENT, PAYMENT_TUTOR, CARD_PAYMENT_INFO,
                    STORE_PRODUCT, STORE_TRANSACTION, STORE_SALE_ITEM,
                    NOTIFICATION, NOTIFICATION_DELIVERY,
                    EMAIL, EMAIL_RECIPIENT, EMAIL_ATTACHMENT,
                    ATTENDANCE_SESSION, ATTENDANCE_RECORD,
                    DEMO_REQUEST,
                    NOTIFICATION_READ_STATUS, PUSH_DEVICE,
                    COURSE_AVAILABLE_COLLABORATOR, ADULT_STUDENT_COURSE, MINOR_STUDENT_COURSE,
                    COURSE_EVENT_ADULT_STUDENT_ATTENDEE, COURSE_EVENT_MINOR_STUDENT_ATTENDEE,
                    MEMBERSHIP_COURSE, COMPENSATION_COLLABORATOR,
                    TENANT_BRANDING, NEWS_FEED_ITEM, TASK,
                    EMAIL_TEMPLATE, EMAIL_TEMPLATE_VARIABLE);
        }

        @Test
        @DisplayName("Should delegate tenant cleaner to tenant use case")
        void shouldDelegateTenantCleaner_toTenantUseCase() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(TENANT).run();

            // Then — state assertion
            assertThat(cleaners).containsKey(TENANT);

            // Then — interaction assertion
            InOrder inOrder = inOrder(tenantUseCase);
            inOrder.verify(tenantUseCase, times(1)).clean();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should delegate tenant sequence cleaner to tenant sequence data clean up")
        void shouldDelegateTenantSequenceCleaner_toTenantSequenceDataCleanUp() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(TENANT_SEQUENCE).run();

            // Then — state assertion
            assertThat(cleaners).containsKey(TENANT_SEQUENCE);

            // Then — interaction assertion
            InOrder inOrder = inOrder(tenantSequenceCleanUp);
            inOrder.verify(tenantSequenceCleanUp, times(1)).clean();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should delegate person PII cleaner to person PII data clean up")
        void shouldDelegatePersonPiiCleaner_toPersonPiiDataCleanUp() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(PERSON_PII).run();

            // Then — state assertion
            assertThat(cleaners).containsKey(PERSON_PII);

            // Then — interaction assertion
            InOrder inOrder = inOrder(personPIICleanUp);
            inOrder.verify(personPIICleanUp, times(1)).clean();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should delegate course cleaner to course use case")
        void shouldDelegateCourseCleaner_toCourseUseCase() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(COURSE).run();

            // Then — state assertion
            assertThat(cleaners).containsKey(COURSE);

            // Then — interaction assertion
            InOrder inOrder = inOrder(courseUseCase);
            inOrder.verify(courseUseCase, times(1)).clean();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should delegate notification cleaner to notification use case")
        void shouldDelegateNotificationCleaner_toNotificationUseCase() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(NOTIFICATION).run();

            // Then — state assertion
            assertThat(cleaners).containsKey(NOTIFICATION);

            // Then — interaction assertion
            InOrder inOrder = inOrder(notificationUseCase);
            inOrder.verify(notificationUseCase, times(1)).clean();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return unmodifiable map")
        void shouldReturnUnmodifiableMap() {
            // Given — cleaners built in setUp

            // When & Then
            assertThatThrownBy(() -> cleaners.put(TENANT, () -> {}))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("mockDataPostLoadHooks bean")
    class MockDataPostLoadHooks {

        private Map<MockEntityType, MockDataPostLoadHook> hooks;

        @BeforeEach
        void setUp() {
            hooks = registry.mockDataPostLoadHooks(
                    tenantRepository, tenantContextHolder,
                    tutorRepository, minorStudentFactory,
                    courseRepository, scheduleRepository, collaboratorRepository,
                    scheduleFactory, courseEventFactory,
                    membershipRepository, adultStudentRepository,
                    membershipAdultStudentFactory, membershipTutorFactory,
                    membershipAdultStudentRepository, membershipTutorRepository,
                    paymentAdultStudentFactory, paymentTutorFactory,
                    paymentAdultStudentRepository, cardPaymentInfoFactory,
                    storeTransactionRepository, storeProductRepository,
                    storeSaleItemFactory,
                    notificationDeliveryFactory, notificationRepository,
                    emailRepository, emailRecipientFactory, emailAttachmentFactory,
                    courseEventRepository,
                    attendanceSessionFactory, attendanceSessionRepository,
                    attendanceRecordFactory,
                    notificationReadStatusFactory, pushDeviceFactory,
                    courseAvailableCollaboratorFactory,
                    adultStudentCourseFactory, minorStudentCourseFactory,
                    courseEventAdultStudentAttendeeFactory, courseEventMinorStudentAttendeeFactory,
                    membershipCourseFactory, compensationCollaboratorFactory,
                    newsFeedItemFactory, taskFactory, emailTemplateVariableFactory,
                    employeeRepository, compensationRepository,
                    minorStudentRepository, emailTemplateRepository);
        }

        @Test
        @DisplayName("Should contain all post-load hooks")
        void shouldContainAllPostLoadHooks() {
            // Given — hooks built in setUp

            // When & Then
            assertThat(hooks).containsOnlyKeys(
                    TENANT, TUTOR, COURSE, SCHEDULE, COLLABORATOR,
                    MEMBERSHIP, ADULT_STUDENT,
                    MEMBERSHIP_ADULT_STUDENT, MEMBERSHIP_TUTOR,
                    PAYMENT_ADULT_STUDENT,
                    STORE_TRANSACTION, STORE_PRODUCT,
                    NOTIFICATION, EMAIL,
                    COURSE_EVENT, ATTENDANCE_SESSION,
                    MINOR_STUDENT, EMPLOYEE, COMPENSATION, EMAIL_TEMPLATE);
        }

        @Test
        @DisplayName("Should set tenant context when tenant hook executes")
        void shouldSetTenantContext_whenTenantHookExecutes() {
            // Given
            TenantDataModel tenant = new TenantDataModel();
            tenant.setTenantId(TENANT_ID);
            when(tenantRepository.findAll()).thenReturn(List.of(tenant));

            // When
            hooks.get(TENANT).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(TENANT);

            // Then — interaction assertions in order: findAll → setTenantId
            InOrder inOrder = inOrder(tenantRepository, tenantContextHolder);
            inOrder.verify(tenantRepository, times(1)).findAll();
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TENANT_ID);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject tutor IDs into factories when tutor hook executes")
        void shouldInjectTutorIds_intoFactories_whenTutorHookExecutes() {
            // Given
            TutorDataModel tutor1 = new TutorDataModel();
            tutor1.setTutorId(TUTOR_ID_ONE);
            TutorDataModel tutor2 = new TutorDataModel();
            tutor2.setTutorId(TUTOR_ID_TWO);
            when(tutorRepository.findAll()).thenReturn(List.of(tutor1, tutor2));

            // When
            hooks.get(TUTOR).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(TUTOR);

            // Then — interaction assertions in order: findAll → inject into factories
            InOrder inOrder = inOrder(tutorRepository, minorStudentFactory, membershipTutorFactory);
            inOrder.verify(tutorRepository, times(1)).findAll();
            inOrder.verify(minorStudentFactory, times(1)).setAvailableTutorIds(List.of(TUTOR_ID_ONE, TUTOR_ID_TWO));
            inOrder.verify(membershipTutorFactory, times(1)).setAvailableTutorIds(List.of(TUTOR_ID_ONE, TUTOR_ID_TWO));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject course IDs into factories when course hook executes")
        void shouldInjectCourseIds_intoFactories_whenCourseHookExecutes() {
            // Given
            CourseDataModel course = new CourseDataModel();
            course.setCourseId(COURSE_ID_ONE);
            when(courseRepository.findAll()).thenReturn(List.of(course));

            // When
            hooks.get(COURSE).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(COURSE);

            // Then — interaction assertions in order: findAll → inject into factories
            InOrder inOrder = inOrder(courseRepository, scheduleFactory, courseEventFactory,
                    membershipAdultStudentFactory, membershipTutorFactory,
                    courseAvailableCollaboratorFactory, adultStudentCourseFactory,
                    minorStudentCourseFactory, membershipCourseFactory, newsFeedItemFactory);
            inOrder.verify(courseRepository, times(1)).findAll();
            inOrder.verify(scheduleFactory, times(1)).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            inOrder.verify(courseEventFactory, times(1)).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            inOrder.verify(membershipAdultStudentFactory, times(1)).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            inOrder.verify(membershipTutorFactory, times(1)).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            inOrder.verify(courseAvailableCollaboratorFactory, times(1)).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            inOrder.verify(adultStudentCourseFactory, times(1)).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            inOrder.verify(minorStudentCourseFactory, times(1)).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            inOrder.verify(membershipCourseFactory, times(1)).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            inOrder.verify(newsFeedItemFactory, times(1)).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject schedule IDs into course event factory when schedule hook executes")
        void shouldInjectScheduleIds_intoCourseEventFactory_whenScheduleHookExecutes() {
            // Given
            ScheduleDataModel schedule = new ScheduleDataModel();
            schedule.setScheduleId(SCHEDULE_ID_ONE);
            when(scheduleRepository.findAll()).thenReturn(List.of(schedule));

            // When
            hooks.get(SCHEDULE).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(SCHEDULE);

            // Then — interaction assertions in order: findAll → inject into factory
            InOrder inOrder = inOrder(scheduleRepository, courseEventFactory);
            inOrder.verify(scheduleRepository, times(1)).findAll();
            inOrder.verify(courseEventFactory, times(1)).setAvailableScheduleIds(List.of(SCHEDULE_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject collaborator IDs into factories when collaborator hook executes")
        void shouldInjectCollaboratorIds_intoFactories_whenCollaboratorHookExecutes() {
            // Given
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            collaborator.setCollaboratorId(COLLABORATOR_ID_ONE);
            when(collaboratorRepository.findAll()).thenReturn(List.of(collaborator));

            // When
            hooks.get(COLLABORATOR).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(COLLABORATOR);

            // Then — interaction assertions in order: findAll → inject into factories
            InOrder inOrder = inOrder(collaboratorRepository, courseEventFactory,
                    courseAvailableCollaboratorFactory, compensationCollaboratorFactory);
            inOrder.verify(collaboratorRepository, times(1)).findAll();
            inOrder.verify(courseEventFactory, times(1)).setAvailableCollaboratorIds(List.of(COLLABORATOR_ID_ONE));
            inOrder.verify(courseAvailableCollaboratorFactory, times(1)).setAvailableCollaboratorIds(List.of(COLLABORATOR_ID_ONE));
            inOrder.verify(compensationCollaboratorFactory, times(1)).setAvailableCollaboratorIds(List.of(COLLABORATOR_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject membership IDs into association factories when membership hook executes")
        void shouldInjectMembershipIds_intoAssociationFactories_whenMembershipHookExecutes() {
            // Given
            MembershipDataModel membership = new MembershipDataModel();
            membership.setMembershipId(MEMBERSHIP_ID_ONE);
            when(membershipRepository.findAll()).thenReturn(List.of(membership));

            // When
            hooks.get(MEMBERSHIP).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(MEMBERSHIP);

            // Then — interaction assertions in order: findAll → inject into factories
            InOrder inOrder = inOrder(membershipRepository, membershipAdultStudentFactory,
                    membershipTutorFactory, membershipCourseFactory);
            inOrder.verify(membershipRepository, times(1)).findAll();
            inOrder.verify(membershipAdultStudentFactory, times(1)).setAvailableMembershipIds(List.of(MEMBERSHIP_ID_ONE));
            inOrder.verify(membershipTutorFactory, times(1)).setAvailableMembershipIds(List.of(MEMBERSHIP_ID_ONE));
            inOrder.verify(membershipCourseFactory, times(1)).setAvailableMembershipIds(List.of(MEMBERSHIP_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject adult student IDs into downstream factories when adult student hook executes")
        void shouldInjectAdultStudentIds_whenAdultStudentHookExecutes() {
            // Given
            AdultStudentDataModel adultStudent = new AdultStudentDataModel();
            adultStudent.setAdultStudentId(ADULT_STUDENT_ID_ONE);
            when(adultStudentRepository.findAll()).thenReturn(List.of(adultStudent));

            // When
            hooks.get(ADULT_STUDENT).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(ADULT_STUDENT);

            // Then — interaction assertions in order: findAll → inject into factories
            InOrder inOrder = inOrder(adultStudentRepository, membershipAdultStudentFactory,
                    attendanceRecordFactory, pushDeviceFactory, notificationReadStatusFactory,
                    adultStudentCourseFactory, courseEventAdultStudentAttendeeFactory);
            inOrder.verify(adultStudentRepository, times(1)).findAll();
            inOrder.verify(membershipAdultStudentFactory, times(1)).setAvailableAdultStudentIds(List.of(ADULT_STUDENT_ID_ONE));
            inOrder.verify(attendanceRecordFactory, times(1)).setAvailableAdultStudentIds(List.of(ADULT_STUDENT_ID_ONE));
            inOrder.verify(pushDeviceFactory, times(1)).setAvailableUserIds(List.of(ADULT_STUDENT_ID_ONE));
            inOrder.verify(notificationReadStatusFactory, times(1)).setAvailableUserIds(List.of(ADULT_STUDENT_ID_ONE));
            inOrder.verify(adultStudentCourseFactory, times(1)).setAvailableAdultStudentIds(List.of(ADULT_STUDENT_ID_ONE));
            inOrder.verify(courseEventAdultStudentAttendeeFactory, times(1)).setAvailableAdultStudentIds(List.of(ADULT_STUDENT_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject membership adult student IDs into payment factory when membership adult student hook executes")
        void shouldInjectMembershipAdultStudentIds_whenMembershipAdultStudentHookExecutes() {
            // Given
            MembershipAdultStudentDataModel mas = new MembershipAdultStudentDataModel();
            mas.setMembershipAdultStudentId(MEMBERSHIP_AS_ID_ONE);
            when(membershipAdultStudentRepository.findAll()).thenReturn(List.of(mas));

            // When
            hooks.get(MEMBERSHIP_ADULT_STUDENT).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(MEMBERSHIP_ADULT_STUDENT);

            // Then — interaction assertions in order: findAll → inject into factory
            InOrder inOrder = inOrder(membershipAdultStudentRepository, paymentAdultStudentFactory);
            inOrder.verify(membershipAdultStudentRepository, times(1)).findAll();
            inOrder.verify(paymentAdultStudentFactory, times(1)).setAvailableMembershipAdultStudentIds(List.of(MEMBERSHIP_AS_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject membership tutor IDs into payment factory when membership tutor hook executes")
        void shouldInjectMembershipTutorIds_whenMembershipTutorHookExecutes() {
            // Given
            MembershipTutorDataModel mt = new MembershipTutorDataModel();
            mt.setMembershipTutorId(MEMBERSHIP_TUTOR_ID_ONE);
            when(membershipTutorRepository.findAll()).thenReturn(List.of(mt));

            // When
            hooks.get(MEMBERSHIP_TUTOR).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(MEMBERSHIP_TUTOR);

            // Then — interaction assertions in order: findAll → inject into factory
            InOrder inOrder = inOrder(membershipTutorRepository, paymentTutorFactory);
            inOrder.verify(membershipTutorRepository, times(1)).findAll();
            inOrder.verify(paymentTutorFactory, times(1)).setAvailableMembershipTutorIds(List.of(MEMBERSHIP_TUTOR_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject payment adult student IDs into card payment info factory when payment adult student hook executes")
        void shouldInjectPaymentIds_intoCardPaymentInfoFactory_whenPaymentAdultStudentHookExecutes() {
            // Given
            PaymentAdultStudentDataModel payment = new PaymentAdultStudentDataModel();
            payment.setPaymentAdultStudentId(PAYMENT_AS_ID_ONE);
            when(paymentAdultStudentRepository.findAll()).thenReturn(List.of(payment));

            // When
            hooks.get(PAYMENT_ADULT_STUDENT).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(PAYMENT_ADULT_STUDENT);

            // Then — interaction assertions in order: findAll → inject into factory
            InOrder inOrder = inOrder(paymentAdultStudentRepository, cardPaymentInfoFactory);
            inOrder.verify(paymentAdultStudentRepository, times(1)).findAll();
            inOrder.verify(cardPaymentInfoFactory, times(1)).setAvailablePaymentAdultStudentIds(List.of(PAYMENT_AS_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject store transaction IDs into store sale item factory when store transaction hook executes")
        void shouldInjectTransactionIds_intoStoreSaleItemFactory_whenStoreTransactionHookExecutes() {
            // Given
            StoreTransactionDataModel transaction = new StoreTransactionDataModel();
            transaction.setStoreTransactionId(STORE_TRANSACTION_ID_ONE);
            when(storeTransactionRepository.findAll()).thenReturn(List.of(transaction));

            // When
            hooks.get(STORE_TRANSACTION).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(STORE_TRANSACTION);

            // Then — interaction assertions in order: findAll → inject into factory
            InOrder inOrder = inOrder(storeTransactionRepository, storeSaleItemFactory);
            inOrder.verify(storeTransactionRepository, times(1)).findAll();
            inOrder.verify(storeSaleItemFactory, times(1)).setAvailableStoreTransactionIds(List.of(STORE_TRANSACTION_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject store product IDs into store sale item factory when store product hook executes")
        void shouldInjectProductIds_intoStoreSaleItemFactory_whenStoreProductHookExecutes() {
            // Given
            StoreProductDataModel product = new StoreProductDataModel();
            product.setStoreProductId(STORE_PRODUCT_ID_ONE);
            when(storeProductRepository.findAll()).thenReturn(List.of(product));

            // When
            hooks.get(STORE_PRODUCT).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(STORE_PRODUCT);

            // Then — interaction assertions in order: findAll → inject into factory
            InOrder inOrder = inOrder(storeProductRepository, storeSaleItemFactory);
            inOrder.verify(storeProductRepository, times(1)).findAll();
            inOrder.verify(storeSaleItemFactory, times(1)).setAvailableStoreProductIds(List.of(STORE_PRODUCT_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject notification IDs into notification delivery factory when notification hook executes")
        void shouldInjectNotificationIds_intoDeliveryFactory_whenNotificationHookExecutes() {
            // Given
            NotificationDataModel notification = new NotificationDataModel();
            notification.setNotificationId(NOTIFICATION_ID_ONE);
            when(notificationRepository.findAll()).thenReturn(List.of(notification));

            // When
            hooks.get(NOTIFICATION).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(NOTIFICATION);

            // Then — interaction assertions in order: findAll → inject into factories
            InOrder inOrder = inOrder(notificationRepository, notificationDeliveryFactory, notificationReadStatusFactory);
            inOrder.verify(notificationRepository, times(1)).findAll();
            inOrder.verify(notificationDeliveryFactory, times(1)).setAvailableNotificationIds(List.of(NOTIFICATION_ID_ONE));
            inOrder.verify(notificationReadStatusFactory, times(1)).setAvailableNotificationIds(List.of(NOTIFICATION_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject email IDs into recipient and attachment factories when email hook executes")
        void shouldInjectEmailIds_intoRecipientAndAttachmentFactories_whenEmailHookExecutes() {
            // Given
            EmailDataModel email = new EmailDataModel();
            email.setEmailId(EMAIL_ID_ONE);
            when(emailRepository.findAll()).thenReturn(List.of(email));

            // When
            hooks.get(EMAIL).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(EMAIL);

            // Then — interaction assertions in order: findAll → inject into factories
            InOrder inOrder = inOrder(emailRepository, emailRecipientFactory, emailAttachmentFactory);
            inOrder.verify(emailRepository, times(1)).findAll();
            inOrder.verify(emailRecipientFactory, times(1)).setAvailableEmailIds(List.of(EMAIL_ID_ONE));
            inOrder.verify(emailAttachmentFactory, times(1)).setAvailableEmailIds(List.of(EMAIL_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject course event IDs into downstream factories when course event hook executes")
        void shouldInjectCourseEventIds_intoFactories_whenCourseEventHookExecutes() {
            // Given
            CourseEventDataModel courseEvent = new CourseEventDataModel();
            courseEvent.setCourseEventId(COURSE_EVENT_ID_ONE);
            when(courseEventRepository.findAll()).thenReturn(List.of(courseEvent));

            // When
            hooks.get(COURSE_EVENT).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(COURSE_EVENT);

            // Then — interaction assertions in order: findAll → inject into factories
            InOrder inOrder = inOrder(courseEventRepository, attendanceSessionFactory,
                    courseEventAdultStudentAttendeeFactory, courseEventMinorStudentAttendeeFactory);
            inOrder.verify(courseEventRepository, times(1)).findAll();
            inOrder.verify(attendanceSessionFactory, times(1)).setAvailableCourseEventIds(List.of(COURSE_EVENT_ID_ONE));
            inOrder.verify(courseEventAdultStudentAttendeeFactory, times(1)).setAvailableCourseEventIds(List.of(COURSE_EVENT_ID_ONE));
            inOrder.verify(courseEventMinorStudentAttendeeFactory, times(1)).setAvailableCourseEventIds(List.of(COURSE_EVENT_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject minor student IDs into downstream factories when minor student hook executes")
        void shouldInjectMinorStudentIds_intoFactories_whenMinorStudentHookExecutes() {
            // Given
            MinorStudentDataModel minorStudent = new MinorStudentDataModel();
            minorStudent.setMinorStudentId(MINOR_STUDENT_ID_ONE);
            when(minorStudentRepository.findAll()).thenReturn(List.of(minorStudent));

            // When
            hooks.get(MINOR_STUDENT).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(MINOR_STUDENT);

            // Then — interaction assertions in order: findAll → inject into factories
            InOrder inOrder = inOrder(minorStudentRepository, minorStudentCourseFactory,
                    courseEventMinorStudentAttendeeFactory);
            inOrder.verify(minorStudentRepository, times(1)).findAll();
            inOrder.verify(minorStudentCourseFactory, times(1)).setAvailableMinorStudentIds(List.of(MINOR_STUDENT_ID_ONE));
            inOrder.verify(courseEventMinorStudentAttendeeFactory, times(1)).setAvailableMinorStudentIds(List.of(MINOR_STUDENT_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject employee IDs into downstream factories when employee hook executes")
        void shouldInjectEmployeeIds_intoFactories_whenEmployeeHookExecutes() {
            // Given
            EmployeeDataModel employee = new EmployeeDataModel();
            employee.setEmployeeId(EMPLOYEE_ID_ONE);
            when(employeeRepository.findAll()).thenReturn(List.of(employee));

            // When
            hooks.get(EMPLOYEE).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(EMPLOYEE);

            // Then — interaction assertions in order: findAll → inject into factories
            InOrder inOrder = inOrder(employeeRepository, newsFeedItemFactory, taskFactory);
            inOrder.verify(employeeRepository, times(1)).findAll();
            inOrder.verify(newsFeedItemFactory, times(1)).setAvailableEmployeeIds(List.of(EMPLOYEE_ID_ONE));
            inOrder.verify(taskFactory, times(1)).setAvailableEmployeeIds(List.of(EMPLOYEE_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject compensation IDs into factory when compensation hook executes")
        void shouldInjectCompensationIds_intoFactory_whenCompensationHookExecutes() {
            // Given
            CompensationDataModel compensation = new CompensationDataModel();
            compensation.setCompensationId(COMPENSATION_ID_ONE);
            when(compensationRepository.findAll()).thenReturn(List.of(compensation));

            // When
            hooks.get(COMPENSATION).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(COMPENSATION);

            // Then — interaction assertions in order: findAll → inject into factory
            InOrder inOrder = inOrder(compensationRepository, compensationCollaboratorFactory);
            inOrder.verify(compensationRepository, times(1)).findAll();
            inOrder.verify(compensationCollaboratorFactory, times(1)).setAvailableCompensationIds(List.of(COMPENSATION_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject email template IDs into factory when email template hook executes")
        void shouldInjectEmailTemplateIds_intoFactory_whenEmailTemplateHookExecutes() {
            // Given
            EmailTemplateDataModel template = new EmailTemplateDataModel();
            template.setTemplateId(EMAIL_TEMPLATE_ID_ONE);
            when(emailTemplateRepository.findAll()).thenReturn(List.of(template));

            // When
            hooks.get(EMAIL_TEMPLATE).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(EMAIL_TEMPLATE);

            // Then — interaction assertions in order: findAll → inject into factory
            InOrder inOrder = inOrder(emailTemplateRepository, emailTemplateVariableFactory);
            inOrder.verify(emailTemplateRepository, times(1)).findAll();
            inOrder.verify(emailTemplateVariableFactory, times(1)).setAvailableTemplateIds(List.of(EMAIL_TEMPLATE_ID_ONE));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should inject attendance session IDs into record factory when attendance session hook executes")
        void shouldInjectAttendanceSessionIds_intoRecordFactory_whenAttendanceSessionHookExecutes() {
            // Given
            AttendanceSessionDataModel session = new AttendanceSessionDataModel();
            session.setAttendanceSessionId(1L);
            when(attendanceSessionRepository.findAll()).thenReturn(List.of(session));

            // When
            hooks.get(ATTENDANCE_SESSION).execute();

            // Then — state assertion
            assertThat(hooks).containsKey(ATTENDANCE_SESSION);

            // Then — interaction assertions in order: findAll → inject into factory
            InOrder inOrder = inOrder(attendanceSessionRepository, attendanceRecordFactory);
            inOrder.verify(attendanceSessionRepository, times(1)).findAll();
            inOrder.verify(attendanceRecordFactory, times(1)).setAvailableAttendanceSessionIds(List.of(1L));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return unmodifiable map")
        void shouldReturnUnmodifiableMap() {
            // Given — hooks built in setUp

            // When & Then
            assertThatThrownBy(() -> hooks.put(TENANT, () -> {}))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
