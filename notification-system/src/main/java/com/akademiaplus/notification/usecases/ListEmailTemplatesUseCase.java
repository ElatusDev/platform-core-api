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
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateListResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Retrieves a list of {@link EmailTemplateDataModel} entities
 * within the current tenant context, with optional category filtering.
 */
@Service
@RequiredArgsConstructor
public class ListEmailTemplatesUseCase {

    private final EmailTemplateRepository emailTemplateRepository;
    private final ModelMapper modelMapper;

    /**
     * Lists all email templates, optionally filtered by category.
     *
     * @param category optional category filter (null returns all)
     * @return the template list response DTO
     */
    public EmailTemplateListResponseDTO list(String category) {
        List<EmailTemplateDataModel> templates;

        if (category != null && !category.isBlank()) {
            templates = emailTemplateRepository.findByCategory(category);
        } else {
            templates = emailTemplateRepository.findAll();
        }

        List<EmailTemplateResponseDTO> templateDTOs = templates.stream()
                .map(t -> modelMapper.map(t, EmailTemplateResponseDTO.class))
                .toList();

        EmailTemplateListResponseDTO response = new EmailTemplateListResponseDTO();
        response.setTemplates(templateDTOs);
        return response;
    }
}
