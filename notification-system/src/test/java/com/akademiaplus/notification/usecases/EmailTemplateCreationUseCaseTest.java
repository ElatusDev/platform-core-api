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
import openapi.akademiaplus.domain.notification.system.dto.CreateEmailTemplateRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.TemplateVariableDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("EmailTemplateCreationUseCase")
@ExtendWith(MockitoExtension.class)
class EmailTemplateCreationUseCaseTest {

    private static final String TEMPLATE_NAME = "Welcome Email";
    private static final String TEMPLATE_DESCRIPTION = "Welcome email for new students";
    private static final String TEMPLATE_CATEGORY = "welcome";
    private static final String TEMPLATE_SUBJECT = "Welcome {{firstName}}!";
    private static final String TEMPLATE_BODY_HTML = "<h1>Hello {{firstName}}</h1>";
    private static final String TEMPLATE_BODY_TEXT = "Hello {{firstName}}";
    private static final Boolean TEMPLATE_IS_ACTIVE = true;

    private static final String VARIABLE_NAME = "firstName";
    private static final String VARIABLE_DESCRIPTION = "Student first name";
    private static final String VARIABLE_DEFAULT_VALUE = "Student";
    private static final Boolean VARIABLE_REQUIRED = true;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    @Mock
    private ModelMapper modelMapper;

    private EmailTemplateCreationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EmailTemplateCreationUseCase(applicationContext, emailTemplateRepository, modelMapper);
    }

    private CreateEmailTemplateRequestDTO buildCreateRequest() {
        CreateEmailTemplateRequestDTO dto = new CreateEmailTemplateRequestDTO();
        dto.setName(TEMPLATE_NAME);
        dto.setDescription(TEMPLATE_DESCRIPTION);
        dto.setCategory(TEMPLATE_CATEGORY);
        dto.setSubject(TEMPLATE_SUBJECT);
        dto.setBodyHtml(TEMPLATE_BODY_HTML);
        dto.setBodyText(TEMPLATE_BODY_TEXT);
        dto.setIsActive(TEMPLATE_IS_ACTIVE);
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
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("Should save and return response when create is called")
        void shouldSaveAndReturnResponse_whenCreateCalled() {
            // Given
            CreateEmailTemplateRequestDTO dto = buildCreateRequest();
            EmailTemplateDataModel template = new EmailTemplateDataModel();
            EmailTemplateDataModel savedTemplate = new EmailTemplateDataModel();
            EmailTemplateResponseDTO expectedResponse = new EmailTemplateResponseDTO();

            when(applicationContext.getBean(EmailTemplateDataModel.class)).thenReturn(template);
            when(emailTemplateRepository.saveAndFlush(template)).thenReturn(savedTemplate);
            when(modelMapper.map(savedTemplate, EmailTemplateResponseDTO.class)).thenReturn(expectedResponse);

            // When
            EmailTemplateResponseDTO result = useCase.create(dto);

            // Then
            assertThat(result).isSameAs(expectedResponse);
            verify(emailTemplateRepository).saveAndFlush(template);
            verify(modelMapper).map(savedTemplate, EmailTemplateResponseDTO.class);
        }
    }

    @Nested
    @DisplayName("Transform")
    class Transform {

        @Test
        @DisplayName("Should build template when DTO is provided")
        void shouldBuildTemplate_whenDtoProvided() {
            // Given
            CreateEmailTemplateRequestDTO dto = buildCreateRequest();
            EmailTemplateDataModel template = new EmailTemplateDataModel();

            when(applicationContext.getBean(EmailTemplateDataModel.class)).thenReturn(template);

            // When
            EmailTemplateDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getName()).isEqualTo(TEMPLATE_NAME);
            assertThat(result.getDescription()).isEqualTo(TEMPLATE_DESCRIPTION);
            assertThat(result.getCategory()).isEqualTo(TEMPLATE_CATEGORY);
            assertThat(result.getSubjectTemplate()).isEqualTo(TEMPLATE_SUBJECT);
            assertThat(result.getBodyHtml()).isEqualTo(TEMPLATE_BODY_HTML);
            assertThat(result.getBodyText()).isEqualTo(TEMPLATE_BODY_TEXT);
            assertThat(result.isActive()).isEqualTo(TEMPLATE_IS_ACTIVE);
        }

        @Test
        @DisplayName("Should build variables when DTO has variables")
        void shouldBuildVariables_whenDtoHasVariables() {
            // Given
            CreateEmailTemplateRequestDTO dto = buildCreateRequest();
            TemplateVariableDTO variableDTO = buildVariableDTO();
            dto.setVariables(List.of(variableDTO));

            EmailTemplateDataModel template = new EmailTemplateDataModel();
            EmailTemplateVariableDataModel variable = new EmailTemplateVariableDataModel();

            when(applicationContext.getBean(EmailTemplateDataModel.class)).thenReturn(template);
            when(applicationContext.getBean(EmailTemplateVariableDataModel.class)).thenReturn(variable);

            // When
            EmailTemplateDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getVariables()).hasSize(1);
            EmailTemplateVariableDataModel resultVariable = result.getVariables().get(0);
            assertThat(resultVariable.getName()).isEqualTo(VARIABLE_NAME);
            assertThat(resultVariable.getVariableType()).isEqualTo(TemplateVariableDTO.TypeEnum.STRING.getValue());
            assertThat(resultVariable.getDescription()).isEqualTo(VARIABLE_DESCRIPTION);
            assertThat(resultVariable.isRequired()).isTrue();
            assertThat(resultVariable.getDefaultValue()).isEqualTo(VARIABLE_DEFAULT_VALUE);
            assertThat(resultVariable.getTemplate()).isSameAs(template);
        }
    }
}
