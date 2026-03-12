/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notifications.email.EmailTemplateDataModel;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplatePreviewRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplatePreviewResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("EmailTemplatePreviewUseCase")
@ExtendWith(MockitoExtension.class)
class EmailTemplatePreviewUseCaseTest {

    private static final Long TEMPLATE_ID = 7L;
    private static final String SUBJECT_TEMPLATE = "Welcome {{firstName}}!";
    private static final String BODY_HTML_TEMPLATE = "<h1>Hello {{firstName}}</h1>";
    private static final String BODY_TEXT_TEMPLATE = "Hello {{firstName}}";
    private static final String RENDERED_SUBJECT = "Welcome John!";
    private static final String RENDERED_BODY_HTML = "<h1>Hello John</h1>";
    private static final String RENDERED_BODY_TEXT = "Hello John";
    private static final String VARIABLE_KEY = "firstName";
    private static final String VARIABLE_VALUE = "John";

    @Mock
    private GetEmailTemplateByIdUseCase getEmailTemplateByIdUseCase;

    @Mock
    private EmailTemplateRenderingService emailTemplateRenderingService;

    private EmailTemplatePreviewUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EmailTemplatePreviewUseCase(getEmailTemplateByIdUseCase, emailTemplateRenderingService);
    }

    private EmailTemplateDataModel buildTemplate(String bodyText) {
        EmailTemplateDataModel template = new EmailTemplateDataModel();
        template.setSubjectTemplate(SUBJECT_TEMPLATE);
        template.setBodyHtml(BODY_HTML_TEMPLATE);
        template.setBodyText(bodyText);
        return template;
    }

    private EmailTemplatePreviewRequestDTO buildPreviewRequest() {
        EmailTemplatePreviewRequestDTO dto = new EmailTemplatePreviewRequestDTO();
        dto.setTemplateData(Map.of(VARIABLE_KEY, VARIABLE_VALUE));
        return dto;
    }

    @Nested
    @DisplayName("Preview")
    class Preview {

        @Test
        @DisplayName("Should render subject and body when variables are provided")
        void shouldRenderSubjectAndBody_whenVariablesProvided() {
            // Given
            EmailTemplateDataModel template = buildTemplate(BODY_TEXT_TEMPLATE);
            EmailTemplatePreviewRequestDTO dto = buildPreviewRequest();
            Map<String, Object> variables = dto.getTemplateData();

            when(getEmailTemplateByIdUseCase.getEntity(TEMPLATE_ID)).thenReturn(template);
            when(emailTemplateRenderingService.render(SUBJECT_TEMPLATE, variables))
                    .thenReturn(RENDERED_SUBJECT);
            when(emailTemplateRenderingService.render(BODY_HTML_TEMPLATE, variables))
                    .thenReturn(RENDERED_BODY_HTML);
            when(emailTemplateRenderingService.render(BODY_TEXT_TEMPLATE, variables))
                    .thenReturn(RENDERED_BODY_TEXT);

            // When
            EmailTemplatePreviewResponseDTO result = useCase.preview(TEMPLATE_ID, dto);

            // Then
            assertThat(result.getSubject()).isEqualTo(RENDERED_SUBJECT);
            assertThat(result.getBodyHtml()).isEqualTo(RENDERED_BODY_HTML);
            assertThat(result.getBodyText()).isEqualTo(RENDERED_BODY_TEXT);
            verify(getEmailTemplateByIdUseCase, times(1)).getEntity(TEMPLATE_ID);
            verify(emailTemplateRenderingService, times(1)).render(SUBJECT_TEMPLATE, variables);
            verify(emailTemplateRenderingService, times(1)).render(BODY_HTML_TEMPLATE, variables);
            verify(emailTemplateRenderingService, times(1)).render(BODY_TEXT_TEMPLATE, variables);
            verifyNoMoreInteractions(getEmailTemplateByIdUseCase, emailTemplateRenderingService);
        }

        @Test
        @DisplayName("Should render body text as null when body text does not exist")
        void shouldRenderBodyText_whenBodyTextExists() {
            // Given
            EmailTemplateDataModel template = buildTemplate(null);
            EmailTemplatePreviewRequestDTO dto = buildPreviewRequest();
            Map<String, Object> variables = dto.getTemplateData();

            when(getEmailTemplateByIdUseCase.getEntity(TEMPLATE_ID)).thenReturn(template);
            when(emailTemplateRenderingService.render(SUBJECT_TEMPLATE, variables))
                    .thenReturn(RENDERED_SUBJECT);
            when(emailTemplateRenderingService.render(BODY_HTML_TEMPLATE, variables))
                    .thenReturn(RENDERED_BODY_HTML);

            // When
            EmailTemplatePreviewResponseDTO result = useCase.preview(TEMPLATE_ID, dto);

            // Then
            assertThat(result.getSubject()).isEqualTo(RENDERED_SUBJECT);
            assertThat(result.getBodyHtml()).isEqualTo(RENDERED_BODY_HTML);
            assertThat(result.getBodyText()).isNull();
            verify(getEmailTemplateByIdUseCase, times(1)).getEntity(TEMPLATE_ID);
            verify(emailTemplateRenderingService, times(1)).render(SUBJECT_TEMPLATE, variables);
            verify(emailTemplateRenderingService, times(1)).render(BODY_HTML_TEMPLATE, variables);
            verify(emailTemplateRenderingService, never()).render(BODY_TEXT_TEMPLATE, variables);
            verifyNoMoreInteractions(getEmailTemplateByIdUseCase, emailTemplateRenderingService);
        }
    }
}
