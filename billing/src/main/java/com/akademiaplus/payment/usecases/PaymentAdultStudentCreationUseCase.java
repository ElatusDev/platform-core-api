/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles adult-student payment creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into the entity ID field and the {@code membershipAdultStudent}
 * FK relationship.
 */
@Service
@RequiredArgsConstructor
public class PaymentAdultStudentCreationUseCase {
    public static final String MAP_NAME = "paymentAdultStudentMap";

    private final ApplicationContext applicationContext;
    private final PaymentAdultStudentRepository paymentRepository;
    private final MembershipAdultStudentRepository membershipAdultStudentRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    @Transactional
    public PaymentAdultStudentCreationResponseDTO create(PaymentAdultStudentCreationRequestDTO dto) {
        PaymentAdultStudentDataModel saved = paymentRepository.saveAndFlush(transform(dto));
        return modelMapper.map(saved, PaymentAdultStudentCreationResponseDTO.class);
    }

    public PaymentAdultStudentDataModel transform(PaymentAdultStudentCreationRequestDTO dto) {
        final PaymentAdultStudentDataModel model =
                applicationContext.getBean(PaymentAdultStudentDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);

        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException("Tenant context is required"));
        MembershipAdultStudentDataModel membershipAdultStudent =
                membershipAdultStudentRepository.findById(
                                new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(tenantId, dto.getMembershipAdultStudentId()))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "MembershipAdultStudent not found: " + dto.getMembershipAdultStudentId()));
        model.setMembershipAdultStudent(membershipAdultStudent);

        return model;
    }
}
