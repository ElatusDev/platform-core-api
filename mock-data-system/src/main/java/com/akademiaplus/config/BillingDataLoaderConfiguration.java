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
import com.akademiaplus.util.mock.billing.CardPaymentInfoFactory.CardPaymentInfoRequest;
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
    public DataLoader<CompensationCreationRequestDTO, CompensationDataModel, Long> compensationDataLoader(
            CompensationRepository repository,
            DataFactory<CompensationCreationRequestDTO> compensationFactory,
            CompensationCreationUseCase compensationCreationUseCase) {

        return new DataLoader<>(repository, compensationCreationUseCase::transform, compensationFactory);
    }

    @Bean
    public DataCleanUp<CompensationDataModel, Long> compensationDataCleanUp(
            EntityManager entityManager,
            CompensationRepository repository) {

        DataCleanUp<CompensationDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(CompensationDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── Membership ──

    @Bean
    public DataLoader<MembershipCreationRequestDTO, MembershipDataModel, Long> membershipDataLoader(
            MembershipRepository repository,
            DataFactory<MembershipCreationRequestDTO> membershipFactory,
            MembershipCreationUseCase membershipCreationUseCase) {

        return new DataLoader<>(repository, membershipCreationUseCase::transform, membershipFactory);
    }

    @Bean
    public DataCleanUp<MembershipDataModel, Long> membershipDataCleanUp(
            EntityManager entityManager,
            MembershipRepository repository) {

        DataCleanUp<MembershipDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(MembershipDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── MembershipAdultStudent ──

    @Bean
    public DataLoader<MembershipAdultStudentCreationRequestDTO, MembershipAdultStudentDataModel, Long>
            membershipAdultStudentDataLoader(
                    MembershipAdultStudentRepository repository,
                    DataFactory<MembershipAdultStudentCreationRequestDTO> factory,
                    MembershipAdultStudentCreationUseCase useCase) {

        return new DataLoader<>(repository, useCase::transform, factory);
    }

    @Bean
    public DataCleanUp<MembershipAdultStudentDataModel, Long> membershipAdultStudentDataCleanUp(
            EntityManager entityManager,
            MembershipAdultStudentRepository repository) {

        DataCleanUp<MembershipAdultStudentDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(MembershipAdultStudentDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── MembershipTutor ──

    @Bean
    public DataLoader<MembershipTutorCreationRequestDTO, MembershipTutorDataModel, Long>
            membershipTutorDataLoader(
                    MembershipTutorRepository repository,
                    DataFactory<MembershipTutorCreationRequestDTO> factory,
                    MembershipTutorCreationUseCase useCase) {

        return new DataLoader<>(repository, useCase::transform, factory);
    }

    @Bean
    public DataCleanUp<MembershipTutorDataModel, Long> membershipTutorDataCleanUp(
            EntityManager entityManager,
            MembershipTutorRepository repository) {

        DataCleanUp<MembershipTutorDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(MembershipTutorDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── PaymentAdultStudent ──

    @Bean
    public DataLoader<PaymentAdultStudentCreationRequestDTO, PaymentAdultStudentDataModel, Long>
            paymentAdultStudentDataLoader(
                    PaymentAdultStudentRepository repository,
                    DataFactory<PaymentAdultStudentCreationRequestDTO> factory,
                    PaymentAdultStudentCreationUseCase useCase) {

        return new DataLoader<>(repository, useCase::transform, factory);
    }

    @Bean
    public DataCleanUp<PaymentAdultStudentDataModel, Long> paymentAdultStudentDataCleanUp(
            EntityManager entityManager,
            PaymentAdultStudentRepository repository) {

        DataCleanUp<PaymentAdultStudentDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(PaymentAdultStudentDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── PaymentTutor ──

    @Bean
    public DataLoader<PaymentTutorCreationRequestDTO, PaymentTutorDataModel, Long>
            paymentTutorDataLoader(
                    PaymentTutorRepository repository,
                    DataFactory<PaymentTutorCreationRequestDTO> factory,
                    PaymentTutorCreationUseCase useCase) {

        return new DataLoader<>(repository, useCase::transform, factory);
    }

    @Bean
    public DataCleanUp<PaymentTutorDataModel, Long> paymentTutorDataCleanUp(
            EntityManager entityManager,
            PaymentTutorRepository repository) {

        DataCleanUp<PaymentTutorDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(PaymentTutorDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── CardPaymentInfo (no domain creation use case — direct mapping) ──

    @Bean
    public DataLoader<CardPaymentInfoRequest, CardPaymentInfoDataModel, Long>
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
    public DataCleanUp<CardPaymentInfoDataModel, Long> cardPaymentInfoDataCleanUp(
            EntityManager entityManager,
            CardPaymentInfoRepository repository) {

        DataCleanUp<CardPaymentInfoDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(CardPaymentInfoDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }
}
