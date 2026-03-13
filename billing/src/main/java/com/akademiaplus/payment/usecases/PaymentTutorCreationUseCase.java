/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.billing.customerpayment.PaymentTutorDataModel;
import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import com.akademiaplus.membership.interfaceadapters.PaymentTutorRepository;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles tutor payment creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into the entity ID field and the {@code membershipTutor}
 * FK relationship.
 */
@Service
@RequiredArgsConstructor
public class PaymentTutorCreationUseCase {
    public static final String MAP_NAME = "paymentTutorMap";
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";
    public static final String ERROR_MEMBERSHIP_TUTOR_NOT_FOUND = "MembershipTutor not found: ";

    private final ApplicationContext applicationContext;
    private final PaymentTutorRepository paymentRepository;
    private final MembershipTutorRepository membershipTutorRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    @Transactional
    public PaymentTutorCreationResponseDTO create(PaymentTutorCreationRequestDTO dto) {
        PaymentTutorDataModel saved = paymentRepository.saveAndFlush(transform(dto));
        return modelMapper.map(saved, PaymentTutorCreationResponseDTO.class);
    }

    public PaymentTutorDataModel transform(PaymentTutorCreationRequestDTO dto) {
        final PaymentTutorDataModel model =
                applicationContext.getBean(PaymentTutorDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);

        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        MembershipTutorDataModel membershipTutor =
                membershipTutorRepository.findById(
                                new MembershipTutorDataModel.MembershipTutorCompositeId(tenantId, dto.getMembershipTutorId()))
                        .orElseThrow(() -> new IllegalArgumentException(
                                ERROR_MEMBERSHIP_TUTOR_NOT_FOUND + dto.getMembershipTutorId()));
        model.setMembershipTutor(membershipTutor);

        return model;
    }
}
