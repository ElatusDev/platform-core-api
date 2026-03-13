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
import com.akademiaplus.task.domain.DomainTask;
import com.akademiaplus.task.domain.exception.TaskAlreadyCompletedException;
import com.akademiaplus.task.interfaceadapters.TaskRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.task.service.dto.CompleteTaskResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("CompleteTaskUseCase")
@ExtendWith(MockitoExtension.class)
class CompleteTaskUseCaseTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private DomainTask domainTask;

    private CompleteTaskUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final Long TASK_ID = 100L;

    @BeforeEach
    void setUp() {
        useCase = new CompleteTaskUseCase(taskRepository, tenantContextHolder, domainTask);
    }

    private TaskDataModel buildEntity(String status) {
        TaskDataModel entity = new TaskDataModel();
        entity.setTenantId(TENANT_ID);
        entity.setTaskId(TASK_ID);
        entity.setTitle("Task");
        entity.setAssigneeId(5L);
        entity.setAssigneeType("EMPLOYEE");
        entity.setDueDate(LocalDate.now().plusDays(3));
        entity.setPriority("MEDIUM");
        entity.setStatus(status);
        entity.setCreatedByUserId(10L);
        return entity;
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Should mark task as completed")
        void shouldMarkCompleted_whenTaskIsPending() {
            // Given
            TaskDataModel entity = buildEntity("PENDING");
            CompleteTaskResponseDTO domainResponse = new CompleteTaskResponseDTO();
            domainResponse.setTaskId(TASK_ID);
            domainResponse.setCompletedAt(OffsetDateTime.now());

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(taskRepository.findById(new TaskId(TENANT_ID, TASK_ID))).thenReturn(Optional.of(entity));
            when(domainTask.get(entity)).thenReturn(domainTask);
            when(domainTask.complete()).thenReturn(domainResponse);
            when(taskRepository.saveAndFlush(entity)).thenReturn(entity);

            // When
            CompleteTaskResponseDTO result = useCase.complete(TASK_ID);

            // Then
            assertThat(result.getTaskId()).isEqualTo(TASK_ID);
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(entity.getStatus()).isEqualTo(DomainTask.COMPLETED_STATUS);
            assertThat(entity.getCompletedAt()).isNotNull();
            verify(taskRepository, times(1)).saveAndFlush(entity);
        }
    }

    @Nested
    @DisplayName("Already Completed")
    class AlreadyCompleted {

        @Test
        @DisplayName("Should throw when task is already completed")
        void shouldThrow_whenTaskAlreadyCompleted() {
            // Given
            TaskDataModel entity = buildEntity(DomainTask.COMPLETED_STATUS);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(taskRepository.findById(new TaskId(TENANT_ID, TASK_ID))).thenReturn(Optional.of(entity));
            when(domainTask.get(entity)).thenReturn(domainTask);
            when(domainTask.complete()).thenThrow(new TaskAlreadyCompletedException(TASK_ID));

            // When/Then
            assertThatThrownBy(() -> useCase.complete(TASK_ID))
                    .isInstanceOf(TaskAlreadyCompletedException.class)
                    .hasMessageContaining(TaskAlreadyCompletedException.ERROR_MESSAGE);

            verify(taskRepository, times(1)).findById(new TaskId(TENANT_ID, TASK_ID));
            verifyNoMoreInteractions(taskRepository);
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
            assertThatThrownBy(() -> useCase.complete(TASK_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.TASK, String.valueOf(TASK_ID)));

            verify(taskRepository, times(1)).findById(new TaskId(TENANT_ID, TASK_ID));
            verifyNoMoreInteractions(taskRepository);
            verifyNoInteractions(domainTask);
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

            // When / Then
            assertThatThrownBy(() -> useCase.complete(TASK_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("No tenant");

            verify(tenantContextHolder, times(1)).requireTenantId();
            verifyNoInteractions(taskRepository, domainTask);
        }

        @Test
        @DisplayName("Should propagate exception when repository findById throws")
        void shouldPropagateException_whenFindByIdThrows() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            RuntimeException dbException = new RuntimeException("DB error");
            when(taskRepository.findById(new TaskId(TENANT_ID, TASK_ID))).thenThrow(dbException);

            // When / Then
            assertThatThrownBy(() -> useCase.complete(TASK_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB error");

            verify(taskRepository, times(1)).findById(new TaskId(TENANT_ID, TASK_ID));
            verifyNoMoreInteractions(taskRepository);
            verifyNoInteractions(domainTask);
        }

        @Test
        @DisplayName("Should propagate exception when saveAndFlush throws")
        void shouldPropagateException_whenSaveAndFlushThrows() {
            // Given
            TaskDataModel entity = buildEntity("PENDING");
            CompleteTaskResponseDTO domainResponse = new CompleteTaskResponseDTO();
            domainResponse.setTaskId(TASK_ID);
            domainResponse.setCompletedAt(OffsetDateTime.now());

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(taskRepository.findById(new TaskId(TENANT_ID, TASK_ID))).thenReturn(Optional.of(entity));
            when(domainTask.get(entity)).thenReturn(domainTask);
            when(domainTask.complete()).thenReturn(domainResponse);
            RuntimeException saveException = new RuntimeException("Save failed");
            when(taskRepository.saveAndFlush(entity)).thenThrow(saveException);

            // When / Then
            assertThatThrownBy(() -> useCase.complete(TASK_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Save failed");

            verify(taskRepository, times(1)).saveAndFlush(entity);
        }
    }
}
