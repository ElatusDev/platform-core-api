/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notifications.email.EmailTemplateDataModel;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplatePreviewRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplatePreviewResponseDTO;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Renders a preview of an email template by substituting
 * provided test data into the template's subject and body placeholders.
 */
@Service
@RequiredArgsConstructor
public class EmailTemplatePreviewUseCase {

    private final GetEmailTemplateByIdUseCase getEmailTemplateByIdUseCase;
    private final EmailTemplateRenderingService emailTemplateRenderingService;

    /**
     * Generates a preview of the specified template with the provided variable values.
     *
     * @param templateId the template identifier
     * @param dto        the preview request containing template data
     * @return the rendered preview response
     */
    public EmailTemplatePreviewResponseDTO preview(Long templateId,
                                                    EmailTemplatePreviewRequestDTO dto) {
        EmailTemplateDataModel template = getEmailTemplateByIdUseCase.getEntity(templateId);
        Map<String, Object> variables = dto.getTemplateData();

        String renderedSubject = emailTemplateRenderingService.render(
                template.getSubjectTemplate(), variables);
        String renderedBodyHtml = emailTemplateRenderingService.render(
                template.getBodyHtml(), variables);
        String renderedBodyText = template.getBodyText() != null
                ? emailTemplateRenderingService.render(template.getBodyText(), variables)
                : null;

        EmailTemplatePreviewResponseDTO response = new EmailTemplatePreviewResponseDTO();
        response.setSubject(renderedSubject);
        response.setBodyHtml(renderedBodyHtml);
        response.setBodyText(renderedBodyText);

        return response;
    }
}
