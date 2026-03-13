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
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.task.service.dto.TaskDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskPriorityDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskStatusDTO;
import openapi.akademiaplus.domain.task.service.dto.UpdateTaskRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("UpdateTaskUseCase")
@ExtendWith(MockitoExtension.class)
class UpdateTaskUseCaseTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TenantContextHolder tenantContextHolder;

    private UpdateTaskUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final Long TASK_ID = 100L;
    private static final String ORIGINAL_TITLE = "Original title";
    private static final String UPDATED_TITLE = "Updated title";

    @BeforeEach
    void setUp() {
        useCase = new UpdateTaskUseCase(taskRepository, tenantContextHolder);
    }

    private TaskDataModel buildEntity() {
        TaskDataModel entity = new TaskDataModel();
        entity.setTenantId(TENANT_ID);
        entity.setTaskId(TASK_ID);
        entity.setTitle(ORIGINAL_TITLE);
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
        @DisplayName("Should update only provided fields")
        void shouldUpdateOnlyProvidedFields_whenUpdating() {
            // Given
            TaskDataModel entity = buildEntity();
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(taskRepository.findById(new TaskId(TENANT_ID, TASK_ID))).thenReturn(Optional.of(entity));
            when(taskRepository.saveAndFlush(entity)).thenReturn(entity);

            UpdateTaskRequestDTO dto = new UpdateTaskRequestDTO();
            dto.setTitle(UPDATED_TITLE);

            // When
            TaskDTO result = useCase.update(TASK_ID, dto);

            // Then
            assertThat(result.getTitle()).isEqualTo(UPDATED_TITLE);
            verify(taskRepository, times(1)).saveAndFlush(entity);
        }

        @Test
        @DisplayName("Should update status when provided")
        void shouldUpdateStatus_whenStatusProvided() {
            // Given
            TaskDataModel entity = buildEntity();
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(taskRepository.findById(new TaskId(TENANT_ID, TASK_ID))).thenReturn(Optional.of(entity));
            when(taskRepository.saveAndFlush(entity)).thenReturn(entity);

            UpdateTaskRequestDTO dto = new UpdateTaskRequestDTO();
            dto.setStatus(TaskStatusDTO.IN_PROGRESS);
            dto.setPriority(TaskPriorityDTO.URGENT);

            // When
            TaskDTO result = useCase.update(TASK_ID, dto);

            // Then
            assertThat(entity.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(entity.getPriority()).isEqualTo("URGENT");
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

            UpdateTaskRequestDTO dto = new UpdateTaskRequestDTO();
            dto.setTitle(UPDATED_TITLE);

            // When/Then
            assertThatThrownBy(() -> useCase.update(TASK_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.TASK, String.valueOf(TASK_ID)));

            verify(taskRepository, times(1)).findById(new TaskId(TENANT_ID, TASK_ID));
            verifyNoMoreInteractions(taskRepository);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when requireTenantId throws")
        void shouldPropagateException_whenRequireTenantIdThrows() {
            // Given
            RuntimeException tenantException = new RuntimeException("No tenant");
            when(tenantContextHolder.requireTenantId()).thenThrow(tenantException);
            UpdateTaskRequestDTO dto = new UpdateTaskRequestDTO();
            dto.setTitle(UPDATED_TITLE);

            // When / Then
            assertThatThrownBy(() -> useCase.update(TASK_ID, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("No tenant");

            verify(tenantContextHolder, times(1)).requireTenantId();
            verifyNoInteractions(taskRepository);
        }

        @Test
        @DisplayName("Should propagate exception when saveAndFlush throws")
        void shouldPropagateException_whenSaveAndFlushThrows() {
            // Given
            TaskDataModel entity = buildEntity();
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(taskRepository.findById(new TaskId(TENANT_ID, TASK_ID))).thenReturn(Optional.of(entity));
            RuntimeException saveException = new RuntimeException("Save failed");
            when(taskRepository.saveAndFlush(entity)).thenThrow(saveException);

            UpdateTaskRequestDTO dto = new UpdateTaskRequestDTO();
            dto.setTitle(UPDATED_TITLE);

            // When / Then
            assertThatThrownBy(() -> useCase.update(TASK_ID, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Save failed");

            verify(taskRepository, times(1)).saveAndFlush(entity);
        }
    }
}
