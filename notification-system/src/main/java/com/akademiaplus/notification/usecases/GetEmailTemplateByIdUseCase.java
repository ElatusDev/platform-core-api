/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.EmailTemplateRepository;
import com.akademiaplus.notifications.email.EmailTemplateDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

/**
 * Retrieves a single {@link EmailTemplateDataModel} by its identifier
 * within the current tenant context.
 */
@Service
@RequiredArgsConstructor
public class GetEmailTemplateByIdUseCase {

    private final EmailTemplateRepository emailTemplateRepository;
    private final ModelMapper modelMapper;

    /**
     * Retrieves the template entity by its identifier.
     *
     * @param templateId the template identifier
     * @return the template data model
     * @throws EntityNotFoundException if no template is found
     */
    public EmailTemplateDataModel getEntity(Long templateId) {
        return emailTemplateRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.EMAIL_TEMPLATE, String.valueOf(templateId)));
    }

    /**
     * Retrieves a template by its identifier and maps to a response DTO.
     *
     * @param templateId the template identifier
     * @return the template response DTO
     * @throws EntityNotFoundException if no template is found
     */
    public EmailTemplateResponseDTO get(Long templateId) {
        EmailTemplateDataModel found = getEntity(templateId);
        return modelMapper.map(found, EmailTemplateResponseDTO.class);
    }
}
