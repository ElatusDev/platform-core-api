/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.usecases;

import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import com.akademiaplus.leadmanagement.interfaceadapters.DemoRequestRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.lead.management.dto.DemoRequestCreationRequestDTO;
import openapi.akademiaplus.domain.lead.management.dto.DemoRequestCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a new demo request after validating email uniqueness.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class DemoRequestCreationUseCase {

    /** Named TypeMap for DTO → DataModel conversion. */
    public static final String MAP_NAME = "demoRequestMap";

    /** Initial status assigned to newly created demo requests. */
    public static final String STATUS_PENDING = "PENDING";

    /** Error constant for duplicate email detection. */
    public static final String ERROR_EMAIL_ALREADY_SUBMITTED =
            "A demo request with this email has already been submitted";

    /** Field name used in {@link DuplicateEntityException}. */
    public static final String FIELD_EMAIL = "email";

    private final DemoRequestRepository demoRequestRepository;
    private final ApplicationContext applicationContext;
    private final ModelMapper modelMapper;

    /**
     * Creates and persists a new demo request.
     *
     * @param dto the creation request containing prospect information
     * @return the created demo request ID
     * @throws DuplicateEntityException if the email has already been submitted
     */
    @Transactional
    public DemoRequestCreationResponseDTO create(DemoRequestCreationRequestDTO dto) {
        if (demoRequestRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateEntityException(EntityType.DEMO_REQUEST, FIELD_EMAIL);
        }

        DemoRequestDataModel model = applicationContext.getBean(DemoRequestDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);
        model.setStatus(STATUS_PENDING);

        DemoRequestDataModel saved = demoRequestRepository.save(model);

        DemoRequestCreationResponseDTO response = new DemoRequestCreationResponseDTO();
        response.setDemoRequestId(saved.getDemoRequestId());
        return response;
    }
}
