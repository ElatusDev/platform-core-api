/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.domain;

import com.akademiaplus.task.TaskDataModel;
import com.akademiaplus.task.domain.exception.TaskAlreadyCompletedException;
import com.akademiaplus.task.domain.exception.TaskDueDateInPastException;
import com.akademiaplus.task.domain.exception.TaskTitleRequiredException;
import openapi.akademiaplus.domain.task.service.dto.CompleteTaskResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DomainTask}.
 *
 * <p>Plain JUnit — no Spring context required.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DomainTask")
class DomainTaskTest {

    private final DomainTask domainTask = new DomainTask();

    private TaskDataModel buildTask(String status, String title, LocalDate dueDate) {
        TaskDataModel task = new TaskDataModel();
        task.setTaskId(1L);
        task.setTenantId(1L);
        task.setTitle(title);
        task.setDueDate(dueDate);
        task.setStatus(status);
        task.setAssigneeId(5L);
        task.setAssigneeType("EMPLOYEE");
        task.setPriority("MEDIUM");
        task.setCreatedByUserId(10L);
        return task;
    }

    @Nested
    @DisplayName("validateTitle")
    class ValidateTitle {

        @Test
        @DisplayName("Should pass when title is present")
        void shouldPass_whenTitleIsPresent() {
            // Given
            TaskDataModel task = buildTask("PENDING", "Valid Title", LocalDate.now().plusDays(1));

            // When / Then — no exception
            domainTask.get(task).validateTitle();
        }

        @Test
        @DisplayName("Should throw TaskTitleRequiredException when title is null")
        void shouldThrowTaskTitleRequiredException_whenTitleIsNull() {
            // Given
            TaskDataModel task = buildTask("PENDING", null, LocalDate.now().plusDays(1));

            // When / Then
            assertThatThrownBy(() -> domainTask.get(task).validateTitle())
                    .isInstanceOf(TaskTitleRequiredException.class)
                    .hasMessage(TaskTitleRequiredException.ERROR_MESSAGE);
        }

        @Test
        @DisplayName("Should throw TaskTitleRequiredException when title is blank")
        void shouldThrowTaskTitleRequiredException_whenTitleIsBlank() {
            // Given
            TaskDataModel task = buildTask("PENDING", "   ", LocalDate.now().plusDays(1));

            // When / Then
            assertThatThrownBy(() -> domainTask.get(task).validateTitle())
                    .isInstanceOf(TaskTitleRequiredException.class)
                    .hasMessage(TaskTitleRequiredException.ERROR_MESSAGE);
        }
    }

    @Nested
    @DisplayName("validateDueDate")
    class ValidateDueDate {

        @Test
        @DisplayName("Should pass when due date is in the future")
        void shouldPass_whenDueDateIsFuture() {
            // Given
            TaskDataModel task = buildTask("PENDING", "Title", LocalDate.now().plusDays(1));

            // When / Then — no exception
            domainTask.get(task).validateDueDate();
        }

        @Test
        @DisplayName("Should pass when due date is today")
        void shouldPass_whenDueDateIsToday() {
            // Given
            TaskDataModel task = buildTask("PENDING", "Title", LocalDate.now());

            // When / Then — no exception
            domainTask.get(task).validateDueDate();
        }

        @Test
        @DisplayName("Should throw TaskDueDateInPastException when due date is in the past")
        void shouldThrowTaskDueDateInPastException_whenDueDateIsPast() {
            // Given
            LocalDate pastDate = LocalDate.now().minusDays(1);
            TaskDataModel task = buildTask("PENDING", "Title", pastDate);

            // When / Then
            assertThatThrownBy(() -> domainTask.get(task).validateDueDate())
                    .isInstanceOf(TaskDueDateInPastException.class)
                    .hasMessageContaining(TaskDueDateInPastException.ERROR_MESSAGE);
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("Should return DTO when task is pending")
        void shouldReturnDTO_whenTaskIsPending() {
            // Given
            TaskDataModel task = buildTask("PENDING", "Title", LocalDate.now().plusDays(1));

            // When
            CompleteTaskResponseDTO response = domainTask.get(task).complete();

            // Then
            assertThat(response.getTaskId()).isEqualTo(1L);
            assertThat(response.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should return DTO when task is in progress")
        void shouldReturnDTO_whenTaskIsInProgress() {
            // Given
            TaskDataModel task = buildTask("IN_PROGRESS", "Title", LocalDate.now().plusDays(1));

            // When
            CompleteTaskResponseDTO response = domainTask.get(task).complete();

            // Then
            assertThat(response.getTaskId()).isEqualTo(1L);
            assertThat(response.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw TaskAlreadyCompletedException when task is completed")
        void shouldThrowTaskAlreadyCompletedException_whenTaskIsCompleted() {
            // Given
            TaskDataModel task = buildTask(DomainTask.COMPLETED_STATUS, "Title",
                    LocalDate.now().plusDays(1));

            // When / Then
            assertThatThrownBy(() -> domainTask.get(task).complete())
                    .isInstanceOf(TaskAlreadyCompletedException.class)
                    .hasMessageContaining(TaskAlreadyCompletedException.ERROR_MESSAGE);
        }
    }

    @Nested
    @DisplayName("fluent chaining")
    class FluentChaining {

        @Test
        @DisplayName("Should allow chaining when all validations pass")
        void shouldAllowChaining_whenAllValidationsPass() {
            // Given
            TaskDataModel task = buildTask("PENDING", "Valid Title", LocalDate.now().plusDays(1));

            // When / Then — no exception
            domainTask.get(task).validateTitle().validateDueDate();
        }
    }
}
