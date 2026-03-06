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
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.TemplateVariableDTO;
import openapi.akademiaplus.domain.notification.system.dto.UpdateEmailTemplateRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("EmailTemplateUpdateUseCase")
@ExtendWith(MockitoExtension.class)
class EmailTemplateUpdateUseCaseTest {

    private static final Long TEMPLATE_ID = 10L;
    private static final String UPDATED_NAME = "Updated Welcome Email";
    private static final String UPDATED_DESCRIPTION = "Updated description";
    private static final String UPDATED_CATEGORY = "billing";
    private static final String UPDATED_SUBJECT = "Updated Subject {{name}}";
    private static final String UPDATED_BODY_HTML = "<h1>Updated {{name}}</h1>";
    private static final String UPDATED_BODY_TEXT = "Updated {{name}}";
    private static final Boolean UPDATED_IS_ACTIVE = false;

    private static final String VARIABLE_NAME = "name";
    private static final String VARIABLE_DESCRIPTION = "Recipient name";
    private static final String VARIABLE_DEFAULT_VALUE = "User";
    private static final Boolean VARIABLE_REQUIRED = true;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    @Mock
    private EmailTemplateVariableRepository emailTemplateVariableRepository;

    @Mock
    private ModelMapper modelMapper;

    private EmailTemplateUpdateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EmailTemplateUpdateUseCase(
                applicationContext, emailTemplateRepository, emailTemplateVariableRepository, modelMapper);
    }

    private UpdateEmailTemplateRequestDTO buildUpdateRequest() {
        UpdateEmailTemplateRequestDTO dto = new UpdateEmailTemplateRequestDTO();
        dto.setName(UPDATED_NAME);
        dto.setDescription(UPDATED_DESCRIPTION);
        dto.setCategory(UPDATED_CATEGORY);
        dto.setSubject(UPDATED_SUBJECT);
        dto.setBodyHtml(UPDATED_BODY_HTML);
        dto.setBodyText(UPDATED_BODY_TEXT);
        dto.setIsActive(UPDATED_IS_ACTIVE);
        return dto;
    }

    private TemplateVariableDTO buildVariableDTO() {
        TemplateVariableDTO variableDTO = new TemplateVariableDTO();
        variableDTO.setName(VARIABLE_NAME);
        variableDTO.setType(TemplateVariableDTO.TypeEnum.STRING);
        variableDTO.setDescription(VARIABLE_DESCRIPTION);
        variableDTO.setRequired(VARIABLE_REQUIRED);
        variableDTO.setDefaultValue(VARIABLE_DEFAULT_VALUE);
        return variableDTO;
    }

    @Nested
    @DisplayName("Update")
    class Update {

        @Test
        @DisplayName("Should update template when found")
        void shouldUpdateTemplate_whenFound() {
            // Given
            UpdateEmailTemplateRequestDTO dto = buildUpdateRequest();
            EmailTemplateDataModel existingTemplate = new EmailTemplateDataModel();
            existingTemplate.setTemplateId(TEMPLATE_ID);
            EmailTemplateDataModel savedTemplate = new EmailTemplateDataModel();
            EmailTemplateResponseDTO expectedResponse = new EmailTemplateResponseDTO();

            when(emailTemplateRepository.findByTemplateId(TEMPLATE_ID))
                    .thenReturn(Optional.of(existingTemplate));
            when(emailTemplateVariableRepository.findByTemplateId(TEMPLATE_ID))
                    .thenReturn(List.of());
            when(emailTemplateRepository.save(existingTemplate)).thenReturn(savedTemplate);
            when(modelMapper.map(savedTemplate, EmailTemplateResponseDTO.class))
                    .thenReturn(expectedResponse);

            // When
            EmailTemplateResponseDTO result = useCase.update(TEMPLATE_ID, dto);

            // Then
            assertThat(result).isSameAs(expectedResponse);
            assertThat(existingTemplate.getName()).isEqualTo(UPDATED_NAME);
            assertThat(existingTemplate.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
            assertThat(existingTemplate.getCategory()).isEqualTo(UPDATED_CATEGORY);
            assertThat(existingTemplate.getSubjectTemplate()).isEqualTo(UPDATED_SUBJECT);
            assertThat(existingTemplate.getBodyHtml()).isEqualTo(UPDATED_BODY_HTML);
            assertThat(existingTemplate.getBodyText()).isEqualTo(UPDATED_BODY_TEXT);
            assertThat(existingTemplate.isActive()).isEqualTo(UPDATED_IS_ACTIVE);
            verify(emailTemplateRepository).save(existingTemplate);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when template not found")
        void shouldThrowEntityNotFound_whenNotFound() {
            // Given
            UpdateEmailTemplateRequestDTO dto = buildUpdateRequest();

            when(emailTemplateRepository.findByTemplateId(TEMPLATE_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.update(TEMPLATE_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(EntityType.EMAIL_TEMPLATE)
                    .hasMessageContaining(String.valueOf(TEMPLATE_ID));
        }

        @Test
        @DisplayName("Should replace variables when template is updated")
        void shouldReplaceVariables_whenUpdated() {
            // Given
            UpdateEmailTemplateRequestDTO dto = buildUpdateRequest();
            TemplateVariableDTO variableDTO = buildVariableDTO();
            dto.setVariables(List.of(variableDTO));

            EmailTemplateDataModel existingTemplate = new EmailTemplateDataModel();
            existingTemplate.setTemplateId(TEMPLATE_ID);
            EmailTemplateVariableDataModel existingVariable = new EmailTemplateVariableDataModel();
            EmailTemplateVariableDataModel newVariable = new EmailTemplateVariableDataModel();
            EmailTemplateDataModel savedTemplate = new EmailTemplateDataModel();
            EmailTemplateResponseDTO expectedResponse = new EmailTemplateResponseDTO();

            when(emailTemplateRepository.findByTemplateId(TEMPLATE_ID))
                    .thenReturn(Optional.of(existingTemplate));
            when(emailTemplateVariableRepository.findByTemplateId(TEMPLATE_ID))
                    .thenReturn(List.of(existingVariable));
            when(applicationContext.getBean(EmailTemplateVariableDataModel.class))
                    .thenReturn(newVariable);
            when(emailTemplateRepository.save(existingTemplate)).thenReturn(savedTemplate);
            when(modelMapper.map(savedTemplate, EmailTemplateResponseDTO.class))
                    .thenReturn(expectedResponse);

            // When
            EmailTemplateResponseDTO result = useCase.update(TEMPLATE_ID, dto);

            // Then
            assertThat(result).isSameAs(expectedResponse);
            verify(emailTemplateVariableRepository).delete(existingVariable);
            assertThat(existingTemplate.getVariables()).hasSize(1);
            assertThat(newVariable.getName()).isEqualTo(VARIABLE_NAME);
            assertThat(newVariable.getVariableType())
                    .isEqualTo(TemplateVariableDTO.TypeEnum.STRING.getValue());
            assertThat(newVariable.getTemplateId()).isEqualTo(TEMPLATE_ID);
            assertThat(newVariable.getTemplate()).isSameAs(existingTemplate);
        }
    }
}
