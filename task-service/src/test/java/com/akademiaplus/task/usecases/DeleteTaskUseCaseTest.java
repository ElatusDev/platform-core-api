/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.task.TaskDataModel;
import com.akademiaplus.task.TaskId;
import com.akademiaplus.task.interfaceadapters.TaskRepository;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DeleteTaskUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteTaskUseCaseTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TenantContextHolder tenantContextHolder;

    private DeleteTaskUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final Long TASK_ID = 100L;

    @BeforeEach
    void setUp() {
        useCase = new DeleteTaskUseCase(taskRepository, tenantContextHolder);
    }

    private TaskDataModel buildEntity() {
        TaskDataModel entity = new TaskDataModel();
        entity.setTenantId(TENANT_ID);
        entity.setTaskId(TASK_ID);
        entity.setTitle("Task to delete");
        entity.setAssigneeId(5L);
        entity.setAssigneeType("EMPLOYEE");
        entity.setDueDate(LocalDate.now().plusDays(3));
        entity.setPriority("MEDIUM");
        entity.setStatus("PENDING");
        entity.setCreatedByUserId(10L);
        return entity;
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Should soft-delete task when it exists")
        void shouldDeleteTask_whenTaskExists() {
            // Given
            TaskDataModel entity = buildEntity();
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(taskRepository.findById(new TaskId(TENANT_ID, TASK_ID))).thenReturn(Optional.of(entity));

            // When
            useCase.delete(TASK_ID);

            // Then
            verify(taskRepository, times(1)).delete(entity);
        }
    }

    @Nested
    @DisplayName("Not Found")
    class NotFound {

        @Test
        @DisplayName("Should throw when task does not exist")
        void shouldThrow_whenTaskNotFound() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(taskRepository.findById(new TaskId(TENANT_ID, TASK_ID))).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> useCase.delete(TASK_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
