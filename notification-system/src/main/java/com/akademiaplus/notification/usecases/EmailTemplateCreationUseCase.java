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
import com.akademiaplus.notifications.email.EmailTemplateVariableDataModel;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.notification.system.dto.CreateEmailTemplateRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.TemplateVariableDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles creation of {@link EmailTemplateDataModel} entities
 * using the two-method pattern.
 * <p>
 * {@link #create} is transactional and persists the template,
 * while {@link #transform} builds the entity graph.
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateCreationUseCase {

    /** ModelMapper type map name for template creation. */
    public static final String MAP_NAME = "emailTemplateCreateMap";

    private final ApplicationContext applicationContext;
    private final EmailTemplateRepository emailTemplateRepository;
    private final ModelMapper modelMapper;

    /**
     * Creates and persists an email template with its variables.
     *
     * @param dto the template creation request
     * @return the created template response DTO
     */
    @Transactional
    public EmailTemplateResponseDTO create(CreateEmailTemplateRequestDTO dto) {
        EmailTemplateDataModel saved = emailTemplateRepository.saveAndFlush(transform(dto));
        return modelMapper.map(saved, EmailTemplateResponseDTO.class);
    }

    /**
     * Transforms the creation request into an {@link EmailTemplateDataModel} entity.
     *
     * @param dto the template creation request
     * @return a populated template data model
     */
    public EmailTemplateDataModel transform(CreateEmailTemplateRequestDTO dto) {
        final EmailTemplateDataModel template = applicationContext.getBean(EmailTemplateDataModel.class);

        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setCategory(dto.getCategory());
        template.setSubjectTemplate(dto.getSubject());
        template.setBodyHtml(dto.getBodyHtml());
        template.setBodyText(dto.getBodyText());
        template.setActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        template.setVariables(buildVariables(dto.getVariables(), template));

        return template;
    }

    private List<EmailTemplateVariableDataModel> buildVariables(
            List<TemplateVariableDTO> variableDTOs,
            EmailTemplateDataModel template) {
        List<EmailTemplateVariableDataModel> variables = new ArrayList<>();

        if (variableDTOs != null) {
            for (TemplateVariableDTO variableDTO : variableDTOs) {
                final EmailTemplateVariableDataModel variable =
                        applicationContext.getBean(EmailTemplateVariableDataModel.class);
                variable.setName(variableDTO.getName());
                variable.setVariableType(variableDTO.getType().getValue());
                variable.setDescription(variableDTO.getDescription());
                variable.setRequired(variableDTO.getRequired() != null && variableDTO.getRequired());
                variable.setDefaultValue(variableDTO.getDefaultValue());
                variable.setTemplate(template);
                variables.add(variable);
            }
        }

        return variables;
    }
}
