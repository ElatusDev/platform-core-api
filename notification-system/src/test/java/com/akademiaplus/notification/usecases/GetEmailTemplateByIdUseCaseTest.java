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
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("GetEmailTemplateByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetEmailTemplateByIdUseCaseTest {

    private static final Long TEMPLATE_ID = 5L;

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    @Mock
    private ModelMapper modelMapper;

    private GetEmailTemplateByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetEmailTemplateByIdUseCase(emailTemplateRepository, modelMapper);
    }

    @Nested
    @DisplayName("GetById")
    class GetById {

        @Test
        @DisplayName("Should return template response when template is found")
        void shouldReturnTemplate_whenFound() {
            // Given
            EmailTemplateDataModel template = new EmailTemplateDataModel();
            EmailTemplateResponseDTO expectedResponse = new EmailTemplateResponseDTO();

            when(emailTemplateRepository.findByTemplateId(TEMPLATE_ID))
                    .thenReturn(Optional.of(template));
            when(modelMapper.map(template, EmailTemplateResponseDTO.class))
                    .thenReturn(expectedResponse);

            // When
            EmailTemplateResponseDTO result = useCase.get(TEMPLATE_ID);

            // Then
            assertThat(result).isSameAs(expectedResponse);
            verify(emailTemplateRepository).findByTemplateId(TEMPLATE_ID);
            verify(modelMapper).map(template, EmailTemplateResponseDTO.class);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when template is not found")
        void shouldThrowEntityNotFound_whenNotFound() {
            // Given
            when(emailTemplateRepository.findByTemplateId(TEMPLATE_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.get(TEMPLATE_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(EntityType.EMAIL_TEMPLATE)
                    .hasMessageContaining(String.valueOf(TEMPLATE_ID));
        }
    }
}
