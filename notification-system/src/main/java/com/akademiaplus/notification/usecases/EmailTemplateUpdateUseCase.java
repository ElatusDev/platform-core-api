/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.EmailTemplateRepository;
import com.akademiaplus.notification.interfaceadapters.EmailTemplateVariableRepository;
import com.akademiaplus.notifications.email.EmailTemplateDataModel;
import com.akademiaplus.notifications.email.EmailTemplateVariableDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.TemplateVariableDTO;
import openapi.akademiaplus.domain.notification.system.dto.UpdateEmailTemplateRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles updates to existing {@link EmailTemplateDataModel} entities,
 * including synchronization of the variable set.
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateUpdateUseCase {

    private final ApplicationContext applicationContext;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailTemplateVariableRepository emailTemplateVariableRepository;
    private final ModelMapper modelMapper;

    /**
     * Updates an existing email template and its variables.
     * <p>
     * Replaces all existing variables with the new set provided in the DTO.
     *
     * @param templateId the template identifier
     * @param dto        the update request
     * @return the updated template response DTO
     * @throws EntityNotFoundException if no template is found
     */
    @Transactional
    public EmailTemplateResponseDTO update(Long templateId, UpdateEmailTemplateRequestDTO dto) {
        EmailTemplateDataModel template = emailTemplateRepository.findByTemplateId(templateId)
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.EMAIL_TEMPLATE, String.valueOf(templateId)));

        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setCategory(dto.getCategory());
        template.setSubjectTemplate(dto.getSubject());
        template.setBodyHtml(dto.getBodyHtml());
        template.setBodyText(dto.getBodyText());
        if (dto.getIsActive() != null) {
            template.setActive(dto.getIsActive());
        }

        // Replace variables: delete existing, create new
        List<EmailTemplateVariableDataModel> existingVariables =
                emailTemplateVariableRepository.findByTemplateId(templateId);
        for (EmailTemplateVariableDataModel existing : existingVariables) {
            emailTemplateVariableRepository.delete(existing);
        }

        List<EmailTemplateVariableDataModel> newVariables = buildVariables(dto.getVariables(), template);
        template.setVariables(newVariables);

        EmailTemplateDataModel saved = emailTemplateRepository.save(template);
        return modelMapper.map(saved, EmailTemplateResponseDTO.class);
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
                variable.setTemplateId(template.getTemplateId());
                variable.setTemplate(template);
                variables.add(variable);
            }
        }

        return variables;
    }
}
