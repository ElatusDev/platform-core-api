/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.billing.dto.MembershipCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles membership creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into the entity ID field and the {@code courses} M2M collection.
 */
@Service
@RequiredArgsConstructor
public class MembershipCreationUseCase {
    public static final String MAP_NAME = "membershipMap";

    private final ApplicationContext applicationContext;
    private final MembershipRepository membershipRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public MembershipCreationResponseDTO create(MembershipCreationRequestDTO dto) {
        MembershipDataModel saved = membershipRepository.saveAndFlush(transform(dto));
        return modelMapper.map(saved, MembershipCreationResponseDTO.class);
    }

    public MembershipDataModel transform(MembershipCreationRequestDTO dto) {
        final MembershipDataModel model = applicationContext.getBean(MembershipDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);
        return model;
    }
}
