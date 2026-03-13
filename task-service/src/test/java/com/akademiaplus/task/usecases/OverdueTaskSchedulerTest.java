/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.usecases;

import com.akademiaplus.task.interfaceadapters.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OverdueTaskScheduler")
@ExtendWith(MockitoExtension.class)
class OverdueTaskSchedulerTest {

    @Mock private TaskRepository taskRepository;

    private OverdueTaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OverdueTaskScheduler(taskRepository);
    }

    @Nested
    @DisplayName("Mark Overdue Tasks")
    class MarkOverdueTasks {

        @Test
        @DisplayName("Should call repository bulk update with current date")
        void shouldCallBulkUpdate_whenSchedulerRuns() {
            // Given
            LocalDate today = LocalDate.now();
            when(taskRepository.markOverdueTasks(today)).thenReturn(3);

            // When
            scheduler.markOverdueTasks();

            // Then
            verify(taskRepository, times(1)).markOverdueTasks(today);
        }

        @Test
        @DisplayName("Should handle zero overdue tasks gracefully")
        void shouldHandleZeroOverdue_whenNoTasksAreOverdue() {
            // Given
            LocalDate today = LocalDate.now();
            when(taskRepository.markOverdueTasks(today)).thenReturn(0);

            // When
            scheduler.markOverdueTasks();

            // Then
            verify(taskRepository, times(1)).markOverdueTasks(today);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when markOverdueTasks throws")
        void shouldPropagateException_whenMarkOverdueTasksThrows() {
            // Given
            LocalDate today = LocalDate.now();
            RuntimeException dbException = new RuntimeException("DB error");
            when(taskRepository.markOverdueTasks(today)).thenThrow(dbException);

            // When / Then
            assertThatThrownBy(() -> scheduler.markOverdueTasks())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");

            verify(taskRepository, times(1)).markOverdueTasks(today);
        }
    }
}
