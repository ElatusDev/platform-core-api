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
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateListResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ListEmailTemplatesUseCase")
@ExtendWith(MockitoExtension.class)
class ListEmailTemplatesUseCaseTest {

    private static final String CATEGORY_WELCOME = "welcome";

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    @Mock
    private ModelMapper modelMapper;

    private ListEmailTemplatesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListEmailTemplatesUseCase(emailTemplateRepository, modelMapper);
    }

    @Nested
    @DisplayName("List")
    class ListTemplates {

        @Test
        @DisplayName("Should return all templates when no category is provided")
        void shouldReturnAllTemplates_whenNoCategoryProvided() {
            // Given
            EmailTemplateDataModel template1 = new EmailTemplateDataModel();
            EmailTemplateDataModel template2 = new EmailTemplateDataModel();
            EmailTemplateResponseDTO responseDTO1 = new EmailTemplateResponseDTO();
            EmailTemplateResponseDTO responseDTO2 = new EmailTemplateResponseDTO();

            when(emailTemplateRepository.findAll()).thenReturn(List.of(template1, template2));
            when(modelMapper.map(template1, EmailTemplateResponseDTO.class)).thenReturn(responseDTO1);
            when(modelMapper.map(template2, EmailTemplateResponseDTO.class)).thenReturn(responseDTO2);

            // When
            EmailTemplateListResponseDTO result = useCase.list(null);

            // Then
            assertThat(result.getTemplates()).hasSize(2);
            assertThat(result.getTemplates()).containsExactly(responseDTO1, responseDTO2);
            verify(emailTemplateRepository, times(1)).findAll();
            verify(emailTemplateRepository, never()).findByCategory(CATEGORY_WELCOME);
            verify(modelMapper, times(1)).map(template1, EmailTemplateResponseDTO.class);
            verify(modelMapper, times(1)).map(template2, EmailTemplateResponseDTO.class);
            verifyNoMoreInteractions(emailTemplateRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return filtered templates when category is provided")
        void shouldReturnFilteredTemplates_whenCategoryProvided() {
            // Given
            EmailTemplateDataModel template = new EmailTemplateDataModel();
            EmailTemplateResponseDTO responseDTO = new EmailTemplateResponseDTO();

            when(emailTemplateRepository.findByCategory(CATEGORY_WELCOME))
                    .thenReturn(List.of(template));
            when(modelMapper.map(template, EmailTemplateResponseDTO.class)).thenReturn(responseDTO);

            // When
            EmailTemplateListResponseDTO result = useCase.list(CATEGORY_WELCOME);

            // Then
            assertThat(result.getTemplates()).hasSize(1);
            assertThat(result.getTemplates()).containsExactly(responseDTO);
            verify(emailTemplateRepository, times(1)).findByCategory(CATEGORY_WELCOME);
            verify(emailTemplateRepository, never()).findAll();
            verify(modelMapper, times(1)).map(template, EmailTemplateResponseDTO.class);
            verifyNoMoreInteractions(emailTemplateRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return empty list when no templates exist")
        void shouldReturnEmptyList_whenNoTemplatesExist() {
            // Given
            when(emailTemplateRepository.findAll()).thenReturn(List.of());

            // When
            EmailTemplateListResponseDTO result = useCase.list(null);

            // Then
            assertThat(result.getTemplates()).isEmpty();
            verify(emailTemplateRepository, times(1)).findAll();
            verifyNoMoreInteractions(emailTemplateRepository, modelMapper);
        }
    }
}
