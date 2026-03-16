/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.billing.customerpayment.CardPaymentInfoDataModel;
import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.billing.customerpayment.PaymentTutorDataModel;
import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.billing.payroll.CompensationDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.CardPaymentInfoRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import com.akademiaplus.membership.interfaceadapters.PaymentTutorRepository;
import com.akademiaplus.membership.usecases.MembershipAdultStudentCreationUseCase;
import com.akademiaplus.membership.usecases.MembershipCreationUseCase;
import com.akademiaplus.membership.usecases.MembershipTutorCreationUseCase;
import com.akademiaplus.payment.usecases.PaymentAdultStudentCreationUseCase;
import com.akademiaplus.payment.usecases.PaymentTutorCreationUseCase;
import com.akademiaplus.payroll.interfaceadapters.CompensationRepository;
import com.akademiaplus.payroll.usecases.CompensationCreationUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.util.base.NativeBridgeDataCleanUp;
import com.akademiaplus.util.base.NativeBridgeDataLoader;
import com.akademiaplus.util.mock.billing.CardPaymentInfoFactory.CardPaymentInfoRequest;
import com.akademiaplus.util.mock.billing.CompensationCollaboratorFactory;
import com.akademiaplus.util.mock.billing.CompensationCollaboratorRecord;
import com.akademiaplus.util.mock.billing.MembershipCourseFactory;
import com.akademiaplus.util.mock.billing.MembershipCourseRecord;
import jakarta.persistence.EntityManager;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationRequestDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for billing-related mock data loader and cleanup beans.
 */
@Configuration
public class BillingDataLoaderConfiguration {

    // ── Compensation ──

    @Bean
    public DataLoader<CompensationCreationRequestDTO, CompensationDataModel, CompensationDataModel.CompensationCompositeId> compensationDataLoader(
            CompensationRepository repository,
            DataFactory<CompensationCreationRequestDTO> compensationFactory,
            CompensationCreationUseCase compensationCreationUseCase) {

        return new DataLoader<>(repository, compensationCreationUseCase::transform, compensationFactory);
    }

    @Bean
    public DataCleanUp<CompensationDataModel, CompensationDataModel.CompensationCompositeId> compensationDataCleanUp(
            EntityManager entityManager,
            CompensationRepository repository) {

        DataCleanUp<CompensationDataModel, CompensationDataModel.CompensationCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(CompensationDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── Membership ──

    @Bean
    public DataLoader<MembershipCreationRequestDTO, MembershipDataModel, MembershipDataModel.MembershipCompositeId> membershipDataLoader(
            MembershipRepository repository,
            DataFactory<MembershipCreationRequestDTO> membershipFactory,
            MembershipCreationUseCase membershipCreationUseCase) {

        return new DataLoader<>(repository, membershipCreationUseCase::transform, membershipFactory);
    }

    @Bean
    public DataCleanUp<MembershipDataModel, MembershipDataModel.MembershipCompositeId> membershipDataCleanUp(
            EntityManager entityManager,
            MembershipRepository repository) {

        DataCleanUp<MembershipDataModel, MembershipDataModel.MembershipCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(MembershipDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── MembershipAdultStudent ──

    @Bean
    public DataLoader<MembershipAdultStudentCreationRequestDTO, MembershipAdultStudentDataModel, MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId>
            membershipAdultStudentDataLoader(
                    MembershipAdultStudentRepository repository,
                    DataFactory<MembershipAdultStudentCreationRequestDTO> factory,
                    MembershipAdultStudentCreationUseCase useCase) {

        return new DataLoader<>(repository, useCase::transform, factory);
    }

    @Bean
    public DataCleanUp<MembershipAdultStudentDataModel, MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId> membershipAdultStudentDataCleanUp(
            EntityManager entityManager,
            MembershipAdultStudentRepository repository) {

        DataCleanUp<MembershipAdultStudentDataModel, MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(MembershipAdultStudentDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── MembershipTutor ──

    @Bean
    public DataLoader<MembershipTutorCreationRequestDTO, MembershipTutorDataModel, MembershipTutorDataModel.MembershipTutorCompositeId>
            membershipTutorDataLoader(
                    MembershipTutorRepository repository,
                    DataFactory<MembershipTutorCreationRequestDTO> factory,
                    MembershipTutorCreationUseCase useCase) {

        return new DataLoader<>(repository, useCase::transform, factory);
    }

    @Bean
    public DataCleanUp<MembershipTutorDataModel, MembershipTutorDataModel.MembershipTutorCompositeId> membershipTutorDataCleanUp(
            EntityManager entityManager,
            MembershipTutorRepository repository) {

        DataCleanUp<MembershipTutorDataModel, MembershipTutorDataModel.MembershipTutorCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(MembershipTutorDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── PaymentAdultStudent ──

    @Bean
    public DataLoader<PaymentAdultStudentCreationRequestDTO, PaymentAdultStudentDataModel, PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId>
            paymentAdultStudentDataLoader(
                    PaymentAdultStudentRepository repository,
                    DataFactory<PaymentAdultStudentCreationRequestDTO> factory,
                    PaymentAdultStudentCreationUseCase useCase) {

        return new DataLoader<>(repository, useCase::transform, factory);
    }

    @Bean
    public DataCleanUp<PaymentAdultStudentDataModel, PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId> paymentAdultStudentDataCleanUp(
            EntityManager entityManager,
            PaymentAdultStudentRepository repository) {

        DataCleanUp<PaymentAdultStudentDataModel, PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(PaymentAdultStudentDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── PaymentTutor ──

    @Bean
    public DataLoader<PaymentTutorCreationRequestDTO, PaymentTutorDataModel, PaymentTutorDataModel.PaymentTutorCompositeId>
            paymentTutorDataLoader(
                    PaymentTutorRepository repository,
                    DataFactory<PaymentTutorCreationRequestDTO> factory,
                    PaymentTutorCreationUseCase useCase) {

        return new DataLoader<>(repository, useCase::transform, factory);
    }

    @Bean
    public DataCleanUp<PaymentTutorDataModel, PaymentTutorDataModel.PaymentTutorCompositeId> paymentTutorDataCleanUp(
            EntityManager entityManager,
            PaymentTutorRepository repository) {

        DataCleanUp<PaymentTutorDataModel, PaymentTutorDataModel.PaymentTutorCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(PaymentTutorDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── CardPaymentInfo (no domain creation use case — direct mapping) ──

    @Bean
    public DataLoader<CardPaymentInfoRequest, CardPaymentInfoDataModel, CardPaymentInfoDataModel.CardPaymentInfoCompositeId>
            cardPaymentInfoDataLoader(
                    CardPaymentInfoRepository repository,
                    DataFactory<CardPaymentInfoRequest> factory,
                    ApplicationContext applicationContext) {

        return new DataLoader<>(repository, dto -> {
            CardPaymentInfoDataModel model = applicationContext.getBean(CardPaymentInfoDataModel.class);
            model.setPaymentId(dto.paymentId());
            model.setToken(dto.token());
            model.setCardType(dto.cardType());
            return model;
        }, factory);
    }

    @Bean
    public DataCleanUp<CardPaymentInfoDataModel, CardPaymentInfoDataModel.CardPaymentInfoCompositeId> cardPaymentInfoDataCleanUp(
            EntityManager entityManager,
            CardPaymentInfoRepository repository) {

        DataCleanUp<CardPaymentInfoDataModel, CardPaymentInfoDataModel.CardPaymentInfoCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(CardPaymentInfoDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── MembershipCourse (bridge table) ──

    /**
     * Creates the native bridge data loader for membership-course records.
     *
     * @param entityManager       the JPA entity manager
     * @param tenantContextHolder the tenant context holder
     * @param factory             the membership-course factory
     * @return a configured native bridge data loader
     */
    @Bean
    public NativeBridgeDataLoader<MembershipCourseRecord> membershipCourseDataLoader(
            EntityManager entityManager,
            TenantContextHolder tenantContextHolder,
            MembershipCourseFactory factory) {

        return new NativeBridgeDataLoader<>(
                entityManager, tenantContextHolder, factory,
                "INSERT INTO membership_courses (tenant_id, membership_id, course_id) VALUES (?, ?, ?)",
                r -> new Object[]{r.membershipId(), r.courseId()});
    }

    /**
     * Creates the native bridge data cleanup for the membership_courses table.
     *
     * @param entityManager the JPA entity manager
     * @return a configured native bridge data cleanup
     */
    @Bean
    public NativeBridgeDataCleanUp membershipCourseDataCleanUp(EntityManager entityManager) {
        NativeBridgeDataCleanUp cleanUp = new NativeBridgeDataCleanUp(entityManager);
        cleanUp.setTableName("membership_courses");
        return cleanUp;
    }

    // ── CompensationCollaborator (bridge table) ──

    /**
     * Creates the native bridge data loader for compensation-collaborator records.
     *
     * @param entityManager       the JPA entity manager
     * @param tenantContextHolder the tenant context holder
     * @param factory             the compensation-collaborator factory
     * @return a configured native bridge data loader
     */
    @Bean
    public NativeBridgeDataLoader<CompensationCollaboratorRecord> compensationCollaboratorDataLoader(
            EntityManager entityManager,
            TenantContextHolder tenantContextHolder,
            CompensationCollaboratorFactory factory) {

        return new NativeBridgeDataLoader<>(
                entityManager, tenantContextHolder, factory,
                "INSERT INTO compensation_collaborators (tenant_id, compensation_id, collaborator_id, assigned_date) VALUES (?, ?, ?, ?)",
                r -> new Object[]{r.compensationId(), r.collaboratorId(), r.assignedDate()});
    }

    /**
     * Creates the native bridge data cleanup for the compensation_collaborators table.
     *
     * @param entityManager the JPA entity manager
     * @return a configured native bridge data cleanup
     */
    @Bean
    public NativeBridgeDataCleanUp compensationCollaboratorDataCleanUp(EntityManager entityManager) {
        NativeBridgeDataCleanUp cleanUp = new NativeBridgeDataCleanUp(entityManager);
        cleanUp.setTableName("compensation_collaborators");
        return cleanUp;
    }
}
