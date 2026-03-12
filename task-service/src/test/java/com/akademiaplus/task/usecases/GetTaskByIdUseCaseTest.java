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
import openapi.akademiaplus.domain.task.service.dto.TaskDTO;
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
import static org.mockito.Mockito.when;

@DisplayName("GetTaskByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetTaskByIdUseCaseTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TenantContextHolder tenantContextHolder;

    private GetTaskByIdUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final Long TASK_ID = 100L;
    private static final String TITLE = "Grade assignments";

    @BeforeEach
    void setUp() {
        useCase = new GetTaskByIdUseCase(taskRepository, tenantContextHolder);
    }

    private TaskDataModel buildEntity() {
        TaskDataModel entity = new TaskDataModel();
        entity.setTenantId(TENANT_ID);
        entity.setTaskId(TASK_ID);
        entity.setTitle(TITLE);
        entity.setAssigneeId(5L);
        entity.setAssigneeType("EMPLOYEE");
        entity.setDueDate(LocalDate.now().plusDays(3));
        entity.setPriority("HIGH");
        entity.setStatus("PENDING");
        entity.setCreatedByUserId(10L);
        return entity;
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Should return task DTO when task exists")
        void shouldReturnTaskDto_whenTaskExists() {
            // Given
            TaskDataModel entity = buildEntity();
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(taskRepository.findById(new TaskId(TENANT_ID, TASK_ID))).thenReturn(Optional.of(entity));

            // When
            TaskDTO result = useCase.get(TASK_ID);

            // Then
            assertThat(result.getId()).isEqualTo(TASK_ID);
            assertThat(result.getTitle()).isEqualTo(TITLE);
            verify(taskRepository, times(1)).findById(new TaskId(TENANT_ID, TASK_ID));
        }
    }

    @Nested
    @DisplayName("Not Found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when task does not exist")
        void shouldThrow_whenTaskNotFound() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(taskRepository.findById(new TaskId(TENANT_ID, TASK_ID))).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> useCase.get(TASK_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
