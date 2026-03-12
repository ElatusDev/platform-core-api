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
import com.akademiaplus.task.interfaceadapters.TaskRepository;
import openapi.akademiaplus.domain.task.service.dto.TaskListResponseDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskPriorityDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ListTasksUseCase")
@ExtendWith(MockitoExtension.class)
class ListTasksUseCaseTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TenantContextHolder tenantContextHolder;

    private ListTasksUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final int PAGE = 0;
    private static final int SIZE = 20;

    @BeforeEach
    void setUp() {
        useCase = new ListTasksUseCase(taskRepository, tenantContextHolder);
    }

    private TaskDataModel buildEntity(Long taskId, String status) {
        TaskDataModel entity = new TaskDataModel();
        entity.setTenantId(TENANT_ID);
        entity.setTaskId(taskId);
        entity.setTitle("Task " + taskId);
        entity.setAssigneeId(5L);
        entity.setAssigneeType("EMPLOYEE");
        entity.setDueDate(LocalDate.now().plusDays(3));
        entity.setPriority("MEDIUM");
        entity.setStatus(status);
        entity.setCreatedByUserId(10L);
        return entity;
    }

    @Nested
    @DisplayName("No Filters")
    class NoFilters {

        @Test
        @DisplayName("Should return all tasks when no filters applied")
        void shouldReturnAllTasks_whenNoFilters() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            List<TaskDataModel> entities = List.of(buildEntity(1L, "PENDING"), buildEntity(2L, "IN_PROGRESS"));
            Page<TaskDataModel> page = new PageImpl<>(entities, PageRequest.of(PAGE, SIZE), 2);
            when(taskRepository.findAllByTenantId(TENANT_ID, PageRequest.of(PAGE, SIZE))).thenReturn(page);

            // When
            TaskListResponseDTO result = useCase.list(null, null, null, null, PAGE, SIZE);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(taskRepository, times(1)).findAllByTenantId(TENANT_ID, PageRequest.of(PAGE, SIZE));
        }

        @Test
        @DisplayName("Should return empty list when no tasks exist")
        void shouldReturnEmptyList_whenNoTasksExist() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            Page<TaskDataModel> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(PAGE, SIZE), 0);
            when(taskRepository.findAllByTenantId(TENANT_ID, PageRequest.of(PAGE, SIZE))).thenReturn(emptyPage);

            // When
            TaskListResponseDTO result = useCase.list(null, null, null, null, PAGE, SIZE);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("With Filters")
    class WithFilters {

        @Test
        @DisplayName("Should filter by status")
        void shouldFilterByStatus_whenStatusProvided() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            Page<TaskDataModel> page = new PageImpl<>(List.of(buildEntity(1L, "PENDING")), PageRequest.of(PAGE, SIZE), 1);
            when(taskRepository.findAllByTenantIdAndStatus(TENANT_ID, "PENDING", PageRequest.of(PAGE, SIZE))).thenReturn(page);

            // When
            TaskListResponseDTO result = useCase.list(TaskStatusDTO.PENDING, null, null, null, PAGE, SIZE);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(taskRepository, times(1)).findAllByTenantIdAndStatus(TENANT_ID, "PENDING", PageRequest.of(PAGE, SIZE));
        }

        @Test
        @DisplayName("Should filter by assignee ID")
        void shouldFilterByAssigneeId_whenAssigneeProvided() {
            // Given
            Long assigneeId = 5L;
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            Page<TaskDataModel> page = new PageImpl<>(List.of(buildEntity(1L, "PENDING")), PageRequest.of(PAGE, SIZE), 1);
            when(taskRepository.findAllByTenantIdAndAssigneeId(TENANT_ID, assigneeId, PageRequest.of(PAGE, SIZE))).thenReturn(page);

            // When
            TaskListResponseDTO result = useCase.list(null, assigneeId, null, null, PAGE, SIZE);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(taskRepository, times(1)).findAllByTenantIdAndAssigneeId(TENANT_ID, assigneeId, PageRequest.of(PAGE, SIZE));
        }

        @Test
        @DisplayName("Should filter by priority")
        void shouldFilterByPriority_whenPriorityProvided() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            Page<TaskDataModel> page = new PageImpl<>(List.of(buildEntity(1L, "PENDING")), PageRequest.of(PAGE, SIZE), 1);
            when(taskRepository.findAllByTenantIdAndPriority(TENANT_ID, "HIGH", PageRequest.of(PAGE, SIZE))).thenReturn(page);

            // When
            TaskListResponseDTO result = useCase.list(null, null, TaskPriorityDTO.HIGH, null, PAGE, SIZE);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(taskRepository, times(1)).findAllByTenantIdAndPriority(TENANT_ID, "HIGH", PageRequest.of(PAGE, SIZE));
        }

        @Test
        @DisplayName("Should filter overdue tasks")
        void shouldFilterOverdue_whenOverdueTrue() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            Page<TaskDataModel> page = new PageImpl<>(List.of(buildEntity(1L, "OVERDUE")), PageRequest.of(PAGE, SIZE), 1);
            when(taskRepository.findOverdueTasks(TENANT_ID, PageRequest.of(PAGE, SIZE))).thenReturn(page);

            // When
            TaskListResponseDTO result = useCase.list(null, null, null, true, PAGE, SIZE);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(taskRepository, times(1)).findOverdueTasks(TENANT_ID, PageRequest.of(PAGE, SIZE));
        }
    }
}
