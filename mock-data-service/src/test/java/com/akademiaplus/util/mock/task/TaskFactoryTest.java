/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.task;

import com.akademiaplus.task.TaskDataModel;
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
 * Unit tests for {@link TaskFactory}.
 */
@DisplayName("TaskFactory")
@ExtendWith(MockitoExtension.class)
class TaskFactoryTest {

    @Mock
    private ApplicationContext applicationContext;

    private TaskFactory factory;

    @BeforeEach
    void setUp() {
        TaskDataGenerator generator = new TaskDataGenerator();
        factory = new TaskFactory(applicationContext, generator);
    }

    private void stubBeanCreation() {
        when(applicationContext.getBean(TaskDataModel.class))
                .thenAnswer(invocation -> new TaskDataModel());
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
            List<TaskDataModel> result = factory.generate(4);

            // Then
            assertThat(result).hasSize(4);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            factory.setAvailableEmployeeIds(List.of(10L));

            // When
            List<TaskDataModel> result = factory.generate(0);

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
                    .hasMessage(TaskFactory.ERROR_EMPLOYEE_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Data model population")
    class DataModelPopulation {

        @Test
        @DisplayName("Should populate required fields when generating single task")
        void shouldPopulateRequiredFields_whenGeneratingSingleTask() {
            // Given
            stubBeanCreation();
            factory.setAvailableEmployeeIds(List.of(10L, 20L));

            // When
            List<TaskDataModel> result = factory.generate(1);
            TaskDataModel model = result.get(0);

            // Then
            assertThat(model.getAssigneeId()).isEqualTo(10L);
            assertThat(model.getCreatedByUserId()).isEqualTo(20L);
            assertThat(model.getTitle()).isNotBlank();
            assertThat(model.getAssigneeType()).isNotBlank();
            assertThat(model.getDueDate()).isNotNull();
            assertThat(model.getPriority()).isNotBlank();
            assertThat(model.getStatus()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Round-robin assignment")
    class RoundRobinAssignment {

        @Test
        @DisplayName("Should use offset of 1 between assignee and creator IDs")
        void shouldUseOffsetOfOne_betweenAssigneeAndCreatorIds() {
            // Given
            stubBeanCreation();
            factory.setAvailableEmployeeIds(List.of(100L, 200L, 300L));

            // When
            List<TaskDataModel> result = factory.generate(3);

            // Then — assignee cycles 0,1,2; creator cycles 1,2,0
            assertThat(result.get(0).getAssigneeId()).isEqualTo(100L);
            assertThat(result.get(0).getCreatedByUserId()).isEqualTo(200L);

            assertThat(result.get(1).getAssigneeId()).isEqualTo(200L);
            assertThat(result.get(1).getCreatedByUserId()).isEqualTo(300L);

            assertThat(result.get(2).getAssigneeId()).isEqualTo(300L);
            assertThat(result.get(2).getCreatedByUserId()).isEqualTo(100L);
        }
    }
}
