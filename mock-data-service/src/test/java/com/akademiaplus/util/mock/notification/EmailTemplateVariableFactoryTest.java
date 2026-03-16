/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.notifications.email.EmailTemplateVariableDataModel;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailTemplateVariableFactory}.
 */
@DisplayName("EmailTemplateVariableFactory")
@ExtendWith(MockitoExtension.class)
class EmailTemplateVariableFactoryTest {

    @Mock
    private ApplicationContext applicationContext;

    private EmailTemplateVariableFactory factory;

    @BeforeEach
    void setUp() {
        factory = new EmailTemplateVariableFactory(applicationContext);
    }

    private void stubBeanCreation() {
        when(applicationContext.getBean(EmailTemplateVariableDataModel.class))
                .thenAnswer(invocation -> new EmailTemplateVariableDataModel());
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndIdsSet() {
            // Given
            stubBeanCreation();
            factory.setAvailableTemplateIds(List.of(10L, 20L));

            // When
            List<EmailTemplateVariableDataModel> result = factory.generate(4);

            // Then
            assertThat(result).hasSize(4);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            factory.setAvailableTemplateIds(List.of(10L));

            // When
            List<EmailTemplateVariableDataModel> result = factory.generate(0);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Stateful constraint validation")
    class StatefulConstraintValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when template IDs are not set")
        void shouldThrowIllegalStateException_whenTemplateIdsNotSet() {
            // Given & When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(EmailTemplateVariableFactory.ERROR_TEMPLATE_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Data model population")
    class DataModelPopulation {

        @Test
        @DisplayName("Should populate required fields when generating single variable")
        void shouldPopulateRequiredFields_whenGeneratingSingleVariable() {
            // Given
            stubBeanCreation();
            factory.setAvailableTemplateIds(List.of(10L));

            // When
            List<EmailTemplateVariableDataModel> result = factory.generate(1);
            EmailTemplateVariableDataModel model = result.get(0);

            // Then
            assertThat(model.getTemplateId()).isEqualTo(10L);
            assertThat(model.getName()).isNotBlank();
            assertThat(model.getVariableType()).isNotBlank();
            assertThat(model.getDescription()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Round-robin assignment")
    class RoundRobinAssignment {

        @Test
        @DisplayName("Should assign template IDs in round-robin fashion")
        void shouldAssignTemplateIds_inRoundRobinFashion() {
            // Given
            stubBeanCreation();
            factory.setAvailableTemplateIds(List.of(100L, 200L));

            // When
            List<EmailTemplateVariableDataModel> result = factory.generate(4);

            // Then
            assertThat(result.get(0).getTemplateId()).isEqualTo(100L);
            assertThat(result.get(1).getTemplateId()).isEqualTo(200L);
            assertThat(result.get(2).getTemplateId()).isEqualTo(100L);
            assertThat(result.get(3).getTemplateId()).isEqualTo(200L);
        }
    }
}
