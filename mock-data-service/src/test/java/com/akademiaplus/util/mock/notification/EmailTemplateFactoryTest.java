/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.notifications.email.EmailTemplateDataModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailTemplateFactory}.
 */
@DisplayName("EmailTemplateFactory")
@ExtendWith(MockitoExtension.class)
class EmailTemplateFactoryTest {

    @Mock
    private ApplicationContext applicationContext;

    private EmailTemplateFactory factory;

    @BeforeEach
    void setUp() {
        EmailTemplateDataGenerator generator = new EmailTemplateDataGenerator();
        factory = new EmailTemplateFactory(applicationContext, generator);
    }

    private void stubBeanCreation() {
        when(applicationContext.getBean(EmailTemplateDataModel.class))
                .thenAnswer(invocation -> new EmailTemplateDataModel());
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number")
        void shouldGenerateExactCount_whenGivenPositiveNumber() {
            // Given
            stubBeanCreation();

            // When
            List<EmailTemplateDataModel> result = factory.generate(5);

            // Then
            assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given & When
            List<EmailTemplateDataModel> result = factory.generate(0);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Data model population")
    class DataModelPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single template")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleTemplate() {
            // Given
            stubBeanCreation();

            // When
            List<EmailTemplateDataModel> result = factory.generate(1);
            EmailTemplateDataModel model = result.get(0);

            // Then
            assertThat(model.getName()).isNotBlank();
            assertThat(model.getDescription()).isNotBlank();
            assertThat(model.getCategory()).isNotBlank();
            assertThat(model.getSubjectTemplate()).isNotBlank();
            assertThat(model.getBodyHtml()).isNotBlank();
            assertThat(model.getBodyText()).isNotBlank();
        }
    }
}
