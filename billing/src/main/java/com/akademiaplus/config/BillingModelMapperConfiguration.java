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
import com.akademiaplus.membership.usecases.MembershipAdultStudentCreationUseCase;
import com.akademiaplus.membership.usecases.MembershipCreationUseCase;
import com.akademiaplus.membership.usecases.MembershipTutorCreationUseCase;
import com.akademiaplus.payment.usecases.PaymentAdultStudentCreationUseCase;
import com.akademiaplus.payment.usecases.PaymentTutorCreationUseCase;
import com.akademiaplus.payroll.usecases.CompensationCreationUseCase;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers module-specific named {@link org.modelmapper.TypeMap TypeMaps}
 * for billing DTO → DataModel conversions.
 * <p>
 * Prevents ModelMapper from deep-matching DTO fields into nested JPA
 * relationships and entity ID fields.
 */
@Configuration
public class BillingModelMapperConfiguration {

    private final ModelMapper modelMapper;

    public BillingModelMapperConfiguration(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    void registerTypeMaps() {
        modelMapper.getConfiguration().setImplicitMappingEnabled(false);

        registerCompensationMap();
        registerMembershipMap();
        registerMembershipAdultStudentMap();
        registerMembershipTutorMap();
        registerPaymentAdultStudentMap();
        registerPaymentTutorMap();

        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }

    private void registerCompensationMap() {
        modelMapper.createTypeMap(
                CompensationCreationRequestDTO.class,
                CompensationDataModel.class,
                CompensationCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(CompensationDataModel::setCompensationId);
            mapper.skip(CompensationDataModel::setCollaborators);
        }).implicitMappings();
    }

    private void registerMembershipMap() {
        modelMapper.createTypeMap(
                MembershipCreationRequestDTO.class,
                MembershipDataModel.class,
                MembershipCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(MembershipDataModel::setMembershipId);
            mapper.skip(MembershipDataModel::setCourses);
        }).implicitMappings();
    }

    private void registerMembershipAdultStudentMap() {
        modelMapper.createTypeMap(
                MembershipAdultStudentCreationRequestDTO.class,
                MembershipAdultStudentDataModel.class,
                MembershipAdultStudentCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(MembershipAdultStudentDataModel::setMembershipAdultStudentId);
            mapper.skip(MembershipAdultStudentDataModel::setMembership);
            mapper.skip(MembershipAdultStudentDataModel::setCourse);
            mapper.skip(MembershipAdultStudentDataModel::setAdultStudent);
        }).implicitMappings();
    }

    private void registerMembershipTutorMap() {
        modelMapper.createTypeMap(
                MembershipTutorCreationRequestDTO.class,
                MembershipTutorDataModel.class,
                MembershipTutorCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(MembershipTutorDataModel::setMembershipTutorId);
            mapper.skip(MembershipTutorDataModel::setMembership);
            mapper.skip(MembershipTutorDataModel::setCourse);
            mapper.skip(MembershipTutorDataModel::setTutor);
        }).implicitMappings();
    }

    private void registerPaymentAdultStudentMap() {
        modelMapper.createTypeMap(
                PaymentAdultStudentCreationRequestDTO.class,
                PaymentAdultStudentDataModel.class,
                PaymentAdultStudentCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(PaymentAdultStudentDataModel::setPaymentAdultStudentId);
            mapper.skip(PaymentAdultStudentDataModel::setMembershipAdultStudent);
        }).implicitMappings();
    }

    private void registerPaymentTutorMap() {
        modelMapper.createTypeMap(
                PaymentTutorCreationRequestDTO.class,
                PaymentTutorDataModel.class,
                PaymentTutorCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(PaymentTutorDataModel::setPaymentTutorId);
            mapper.skip(PaymentTutorDataModel::setMembershipTutor);
        }).implicitMappings();
    }
}
