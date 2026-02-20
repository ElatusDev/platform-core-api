/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
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
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.mock.billing.CardPaymentInfoFactory;
import com.akademiaplus.util.mock.billing.MembershipAdultStudentFactory;
import com.akademiaplus.util.mock.billing.MembershipTutorFactory;
import com.akademiaplus.util.mock.billing.PaymentAdultStudentFactory;
import com.akademiaplus.util.mock.billing.PaymentTutorFactory;
import com.akademiaplus.util.mock.course.CourseEventFactory;
import com.akademiaplus.util.mock.course.ScheduleFactory;
import com.akademiaplus.util.mock.notification.EmailAttachmentFactory;
import com.akademiaplus.util.mock.notification.EmailRecipientFactory;
import com.akademiaplus.util.mock.notification.NotificationDeliveryFactory;
import com.akademiaplus.util.mock.store.StoreSaleItemFactory;
import com.akademiaplus.util.mock.users.MinorStudentFactory;
import com.akademiaplus.utilities.idgeneration.interfaceadapters.TenantSequence;
import com.akademiaplus.usecases.billing.LoadCardPaymentInfoMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadCompensationMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadMembershipAdultStudentMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadMembershipMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadMembershipTutorMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadPaymentAdultStudentMockDataUseCase;
import com.akademiaplus.usecases.billing.LoadPaymentTutorMockDataUseCase;
import com.akademiaplus.usecases.course.LoadCourseEventMockDataUseCase;
import com.akademiaplus.usecases.course.LoadCourseMockDataUseCase;
import com.akademiaplus.usecases.course.LoadScheduleMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailAttachmentMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadEmailRecipientMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadNotificationDeliveryMockDataUseCase;
import com.akademiaplus.usecases.notification.LoadNotificationMockDataUseCase;
import com.akademiaplus.usecases.store.LoadStoreSaleItemMockDataUseCase;
import com.akademiaplus.usecases.store.LoadStoreProductMockDataUseCase;
import com.akademiaplus.usecases.store.LoadStoreTransactionMockDataUseCase;
import com.akademiaplus.usecases.tenant.LoadTenantBillingCycleMockDataUseCase;
import com.akademiaplus.usecases.tenant.LoadTenantSubscriptionMockDataUseCase;
import com.akademiaplus.usecases.users.LoadAdultStudentMockDataUseCase;
import com.akademiaplus.usecases.users.LoadCollaboratorMockDataUseCase;
import com.akademiaplus.usecases.users.LoadEmployeeMockDataUseCase;
import com.akademiaplus.usecases.users.LoadMinorStudentMockDataUseCase;
import com.akademiaplus.usecases.users.LoadTenantMockDataUseCase;
import com.akademiaplus.usecases.users.LoadTutorMockDataUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import static com.akademiaplus.config.MockEntityType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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

    // ── Cleanup-only beans ──
    @Mock private DataCleanUp<TenantSequence, TenantSequence.TenantSequenceId> tenantSequenceCleanUp;
    @Mock private DataCleanUp<InternalAuthDataModel, Long> internalAuthCleanUp;
    @Mock private DataCleanUp<CustomerAuthDataModel, Long> customerAuthCleanUp;
    @Mock private DataCleanUp<PersonPIIDataModel, Long> personPIICleanUp;

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
                    emailUseCase, emailRecipientUseCase, emailAttachmentUseCase);
        }

        @Test
        @DisplayName("Should contain exactly the twenty-six loadable entity types")
        void shouldContainExactlyTwentySixLoadableEntityTypes() {
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
                    EMAIL, EMAIL_RECIPIENT, EMAIL_ATTACHMENT);
        }

        @Test
        @DisplayName("Should delegate tenant loader to tenant use case")
        void shouldDelegateTenantLoader_toTenantUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(TENANT).accept(RECORD_COUNT);

            // Then
            verify(tenantUseCase).load(RECORD_COUNT);
        }

        @Test
        @DisplayName("Should delegate course loader to course use case")
        void shouldDelegateCourseLoader_toCourseUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(COURSE).accept(RECORD_COUNT);

            // Then
            verify(courseUseCase).load(RECORD_COUNT);
        }

        @Test
        @DisplayName("Should delegate compensation loader to compensation use case")
        void shouldDelegateCompensationLoader_toCompensationUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(COMPENSATION).accept(RECORD_COUNT);

            // Then
            verify(compensationUseCase).load(RECORD_COUNT);
        }

        @Test
        @DisplayName("Should delegate notification loader to notification use case")
        void shouldDelegateNotificationLoader_toNotificationUseCase() {
            // Given — loaders built in setUp

            // When
            loaders.get(NOTIFICATION).accept(RECORD_COUNT);

            // Then
            verify(notificationUseCase).load(RECORD_COUNT);
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
                    customerAuthCleanUp, personPIICleanUp);
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
                    EMAIL, EMAIL_RECIPIENT, EMAIL_ATTACHMENT);
        }

        @Test
        @DisplayName("Should delegate tenant cleaner to tenant use case")
        void shouldDelegateTenantCleaner_toTenantUseCase() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(TENANT).run();

            // Then
            verify(tenantUseCase).clean();
        }

        @Test
        @DisplayName("Should delegate tenant sequence cleaner to tenant sequence data clean up")
        void shouldDelegateTenantSequenceCleaner_toTenantSequenceDataCleanUp() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(TENANT_SEQUENCE).run();

            // Then
            verify(tenantSequenceCleanUp).clean();
        }

        @Test
        @DisplayName("Should delegate person PII cleaner to person PII data clean up")
        void shouldDelegatePersonPiiCleaner_toPersonPiiDataCleanUp() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(PERSON_PII).run();

            // Then
            verify(personPIICleanUp).clean();
        }

        @Test
        @DisplayName("Should delegate course cleaner to course use case")
        void shouldDelegateCourseCleaner_toCourseUseCase() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(COURSE).run();

            // Then
            verify(courseUseCase).clean();
        }

        @Test
        @DisplayName("Should delegate notification cleaner to notification use case")
        void shouldDelegateNotificationCleaner_toNotificationUseCase() {
            // Given — cleaners built in setUp

            // When
            cleaners.get(NOTIFICATION).run();

            // Then
            verify(notificationUseCase).clean();
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
                    emailRepository, emailRecipientFactory, emailAttachmentFactory);
        }

        @Test
        @DisplayName("Should contain all fourteen post-load hooks")
        void shouldContainAllFourteenPostLoadHooks() {
            // Given — hooks built in setUp

            // When & Then
            assertThat(hooks).containsOnlyKeys(
                    TENANT, TUTOR, COURSE, SCHEDULE, COLLABORATOR,
                    MEMBERSHIP, ADULT_STUDENT,
                    MEMBERSHIP_ADULT_STUDENT, MEMBERSHIP_TUTOR,
                    PAYMENT_ADULT_STUDENT,
                    STORE_TRANSACTION, STORE_PRODUCT,
                    NOTIFICATION, EMAIL);
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

            // Then
            verify(tenantRepository).findAll();
            verify(tenantContextHolder).setTenantId(TENANT_ID);
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

            // Then
            verify(tutorRepository).findAll();
            verify(minorStudentFactory).setAvailableTutorIds(List.of(TUTOR_ID_ONE, TUTOR_ID_TWO));
            verify(membershipTutorFactory).setAvailableTutorIds(List.of(TUTOR_ID_ONE, TUTOR_ID_TWO));
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

            // Then
            verify(courseRepository).findAll();
            verify(scheduleFactory).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            verify(courseEventFactory).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            verify(membershipAdultStudentFactory).setAvailableCourseIds(List.of(COURSE_ID_ONE));
            verify(membershipTutorFactory).setAvailableCourseIds(List.of(COURSE_ID_ONE));
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

            // Then
            verify(scheduleRepository).findAll();
            verify(courseEventFactory).setAvailableScheduleIds(List.of(SCHEDULE_ID_ONE));
        }

        @Test
        @DisplayName("Should inject collaborator IDs into course event factory when collaborator hook executes")
        void shouldInjectCollaboratorIds_intoCourseEventFactory_whenCollaboratorHookExecutes() {
            // Given
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            collaborator.setCollaboratorId(COLLABORATOR_ID_ONE);
            when(collaboratorRepository.findAll()).thenReturn(List.of(collaborator));

            // When
            hooks.get(COLLABORATOR).execute();

            // Then
            verify(collaboratorRepository).findAll();
            verify(courseEventFactory).setAvailableCollaboratorIds(List.of(COLLABORATOR_ID_ONE));
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

            // Then
            verify(membershipRepository).findAll();
            verify(membershipAdultStudentFactory).setAvailableMembershipIds(List.of(MEMBERSHIP_ID_ONE));
            verify(membershipTutorFactory).setAvailableMembershipIds(List.of(MEMBERSHIP_ID_ONE));
        }

        @Test
        @DisplayName("Should inject adult student IDs into membership adult student factory when adult student hook executes")
        void shouldInjectAdultStudentIds_whenAdultStudentHookExecutes() {
            // Given
            AdultStudentDataModel adultStudent = new AdultStudentDataModel();
            adultStudent.setAdultStudentId(ADULT_STUDENT_ID_ONE);
            when(adultStudentRepository.findAll()).thenReturn(List.of(adultStudent));

            // When
            hooks.get(ADULT_STUDENT).execute();

            // Then
            verify(adultStudentRepository).findAll();
            verify(membershipAdultStudentFactory).setAvailableAdultStudentIds(List.of(ADULT_STUDENT_ID_ONE));
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

            // Then
            verify(membershipAdultStudentRepository).findAll();
            verify(paymentAdultStudentFactory).setAvailableMembershipAdultStudentIds(List.of(MEMBERSHIP_AS_ID_ONE));
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

            // Then
            verify(membershipTutorRepository).findAll();
            verify(paymentTutorFactory).setAvailableMembershipTutorIds(List.of(MEMBERSHIP_TUTOR_ID_ONE));
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

            // Then
            verify(paymentAdultStudentRepository).findAll();
            verify(cardPaymentInfoFactory).setAvailablePaymentAdultStudentIds(List.of(PAYMENT_AS_ID_ONE));
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

            // Then
            verify(storeTransactionRepository).findAll();
            verify(storeSaleItemFactory).setAvailableStoreTransactionIds(List.of(STORE_TRANSACTION_ID_ONE));
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

            // Then
            verify(storeProductRepository).findAll();
            verify(storeSaleItemFactory).setAvailableStoreProductIds(List.of(STORE_PRODUCT_ID_ONE));
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

            // Then
            verify(notificationRepository).findAll();
            verify(notificationDeliveryFactory).setAvailableNotificationIds(List.of(NOTIFICATION_ID_ONE));
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

            // Then
            verify(emailRepository).findAll();
            verify(emailRecipientFactory).setAvailableEmailIds(List.of(EMAIL_ID_ONE));
            verify(emailAttachmentFactory).setAvailableEmailIds(List.of(EMAIL_ID_ONE));
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
