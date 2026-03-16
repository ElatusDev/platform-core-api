/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.newsfeed.NewsFeedItemDataModel;
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
 * Unit tests for {@link NewsFeedItemFactory}.
 */
@DisplayName("NewsFeedItemFactory")
@ExtendWith(MockitoExtension.class)
class NewsFeedItemFactoryTest {

    @Mock
    private ApplicationContext applicationContext;

    private NewsFeedItemFactory factory;

    @BeforeEach
    void setUp() {
        NewsFeedItemDataGenerator generator = new NewsFeedItemDataGenerator();
        factory = new NewsFeedItemFactory(applicationContext, generator);
    }

    private void stubBeanCreation() {
        when(applicationContext.getBean(NewsFeedItemDataModel.class))
                .thenAnswer(invocation -> new NewsFeedItemDataModel());
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndIdsSet() {
            // Given
            stubBeanCreation();
            factory.setAvailableEmployeeIds(List.of(10L, 20L));

            // When
            List<NewsFeedItemDataModel> result = factory.generate(4);

            // Then
            assertThat(result).hasSize(4);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            factory.setAvailableEmployeeIds(List.of(10L));

            // When
            List<NewsFeedItemDataModel> result = factory.generate(0);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Stateful constraint validation")
    class StatefulConstraintValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when employee IDs are not set")
        void shouldThrowIllegalStateException_whenEmployeeIdsNotSet() {
            // Given & When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(NewsFeedItemFactory.ERROR_EMPLOYEE_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Data model population")
    class DataModelPopulation {

        @Test
        @DisplayName("Should populate required fields when generating single item")
        void shouldPopulateRequiredFields_whenGeneratingSingleItem() {
            // Given
            stubBeanCreation();
            factory.setAvailableEmployeeIds(List.of(10L));

            // When
            List<NewsFeedItemDataModel> result = factory.generate(1);
            NewsFeedItemDataModel model = result.get(0);

            // Then
            assertThat(model.getAuthorId()).isEqualTo(10L);
            assertThat(model.getTitle()).isNotBlank();
            assertThat(model.getBody()).isNotBlank();
            assertThat(model.getStatus()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Round-robin assignment")
    class RoundRobinAssignment {

        @Test
        @DisplayName("Should assign employee IDs in round-robin fashion")
        void shouldAssignEmployeeIds_inRoundRobinFashion() {
            // Given
            stubBeanCreation();
            factory.setAvailableEmployeeIds(List.of(100L, 200L));

            // When
            List<NewsFeedItemDataModel> result = factory.generate(4);

            // Then
            assertThat(result.get(0).getAuthorId()).isEqualTo(100L);
            assertThat(result.get(1).getAuthorId()).isEqualTo(200L);
            assertThat(result.get(2).getAuthorId()).isEqualTo(100L);
            assertThat(result.get(3).getAuthorId()).isEqualTo(200L);
        }
    }
}
