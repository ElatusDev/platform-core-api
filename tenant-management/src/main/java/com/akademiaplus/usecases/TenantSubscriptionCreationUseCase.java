/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantSubscriptionRepository;
import com.akademiaplus.tenancy.TenantSubscriptionDataModel;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.tenant.management.dto.SubscriptionCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantSubscriptionDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles tenant subscription creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into the entity ID field.
 */
@Service
@RequiredArgsConstructor
public class TenantSubscriptionCreationUseCase {
    public static final String MAP_NAME = "tenantSubscriptionMap";

    private final ApplicationContext applicationContext;
    private final TenantSubscriptionRepository repository;
    private final ModelMapper modelMapper;

    /**
     * Creates and persists a new tenant subscription from the given request.
     *
     * @param dto the subscription creation request
     * @return the persisted subscription mapped to a response DTO
     */
    @Transactional
    public TenantSubscriptionDTO create(SubscriptionCreateRequestDTO dto) {
        TenantSubscriptionDataModel saved = repository.saveAndFlush(transform(dto));
        return modelMapper.map(saved, TenantSubscriptionDTO.class);
    }

    /**
     * Maps a {@link SubscriptionCreateRequestDTO} to a persistence-ready data model.
     * <p>
     * Uses a named TypeMap to prevent deep-matching of DTO fields
     * into the entity ID. The subscription type enum is mapped by
     * ModelMapper's implicit conversion from the DTO's string-backed enum.
     * <p>
     * The {@code maxUsers} field is a {@code JsonNullable<Integer>} in the DTO
     * (OpenAPI {@code nullable: true}) and is skipped by the TypeMap to avoid
     * ModelMapper's inability to convert {@code JsonNullable} wrappers. It is
     * manually unwrapped here instead.
     *
     * @param dto the creation request
     * @return populated data model ready for persistence
     */
    public TenantSubscriptionDataModel transform(SubscriptionCreateRequestDTO dto) {
        final TenantSubscriptionDataModel model =
                applicationContext.getBean(TenantSubscriptionDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);

        if (dto.getMaxUsers() != null && dto.getMaxUsers().isPresent()) {
            model.setMaxUsers(dto.getMaxUsers().get());
        }

        return model;
    }
}
