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
import openapi.akademiaplus.domain.task.service.dto.AssigneeTypeDTO;
import openapi.akademiaplus.domain.task.service.dto.CreateTaskRequestDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskPriorityDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CreateTaskUseCase")
@ExtendWith(MockitoExtension.class)
class CreateTaskUseCaseTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ApplicationContext applicationContext;

    private CreateTaskUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long TASK_ID = 100L;
    private static final String TITLE = "Review homework";
    private static final String DESCRIPTION = "Review student homework submissions";
    private static final Long ASSIGNEE_ID = 5L;

    @BeforeEach
    void setUp() {
        useCase = new CreateTaskUseCase(taskRepository, tenantContextHolder, applicationContext);
    }

    private CreateTaskRequestDTO buildDto() {
        CreateTaskRequestDTO dto = new CreateTaskRequestDTO();
        dto.setTitle(TITLE);
        dto.setDescription(DESCRIPTION);
        dto.setAssigneeId(ASSIGNEE_ID);
        dto.setAssigneeType(AssigneeTypeDTO.EMPLOYEE);
        dto.setDueDate(LocalDate.now().plusDays(7));
        dto.setPriority(TaskPriorityDTO.HIGH);
        return dto;
    }

    private TaskDataModel buildSavedEntity() {
        TaskDataModel entity = new TaskDataModel();
        entity.setTenantId(TENANT_ID);
        entity.setTaskId(TASK_ID);
        entity.setTitle(TITLE);
        entity.setDescription(DESCRIPTION);
        entity.setAssigneeId(ASSIGNEE_ID);
        entity.setAssigneeType(AssigneeTypeDTO.EMPLOYEE.getValue());
        entity.setDueDate(LocalDate.now().plusDays(7));
        entity.setPriority(TaskPriorityDTO.HIGH.getValue());
        entity.setStatus(TaskDataModel.DEFAULT_STATUS);
        entity.setCreatedByUserId(USER_ID);
        return entity;
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Should create task with default PENDING status")
        void shouldCreateTask_whenValidRequest() {
            // Given
            CreateTaskRequestDTO dto = buildDto();
            TaskDataModel prototypeBean = new TaskDataModel();
            TaskDataModel savedEntity = buildSavedEntity();
            when(applicationContext.getBean(TaskDataModel.class)).thenReturn(prototypeBean);
            when(taskRepository.saveAndFlush(prototypeBean)).thenReturn(savedEntity);

            // When
            TaskDTO result = useCase.create(dto, USER_ID);

            // Then
            assertThat(result.getId()).isEqualTo(TASK_ID);
            assertThat(result.getTitle()).isEqualTo(TITLE);
            assertThat(result.getStatus().getValue()).isEqualTo(TaskDataModel.DEFAULT_STATUS);
            verify(taskRepository, times(1)).saveAndFlush(prototypeBean);
        }

        @Test
        @DisplayName("Should set createdByUserId from parameter")
        void shouldSetCreatedByUserId_whenCreating() {
            // Given
            CreateTaskRequestDTO dto = buildDto();
            TaskDataModel prototypeBean = new TaskDataModel();
            TaskDataModel savedEntity = buildSavedEntity();
            when(applicationContext.getBean(TaskDataModel.class)).thenReturn(prototypeBean);
            when(taskRepository.saveAndFlush(prototypeBean)).thenReturn(savedEntity);

            // When
            useCase.create(dto, USER_ID);

            // Then
            ArgumentCaptor<TaskDataModel> captor = ArgumentCaptor.forClass(TaskDataModel.class);
            verify(taskRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getCreatedByUserId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should throw when title is blank")
        void shouldThrow_whenTitleIsBlank() {
            // Given
            CreateTaskRequestDTO dto = buildDto();
            dto.setTitle("  ");

            // When/Then
            assertThatThrownBy(() -> useCase.create(dto, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(CreateTaskUseCase.ERROR_TITLE_REQUIRED);
        }

        @Test
        @DisplayName("Should throw when title is null")
        void shouldThrow_whenTitleIsNull() {
            // Given
            CreateTaskRequestDTO dto = buildDto();
            dto.setTitle(null);

            // When/Then
            assertThatThrownBy(() -> useCase.create(dto, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(CreateTaskUseCase.ERROR_TITLE_REQUIRED);
        }

        @Test
        @DisplayName("Should throw when due date is in the past")
        void shouldThrow_whenDueDateInPast() {
            // Given
            CreateTaskRequestDTO dto = buildDto();
            dto.setDueDate(LocalDate.now().minusDays(1));

            // When/Then
            assertThatThrownBy(() -> useCase.create(dto, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(CreateTaskUseCase.ERROR_DUE_DATE_IN_PAST);
        }
    }
}
