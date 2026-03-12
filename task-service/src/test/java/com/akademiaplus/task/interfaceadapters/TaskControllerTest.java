/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.interfaceadapters;

import com.akademiaplus.task.usecases.CompleteTaskUseCase;
import com.akademiaplus.task.usecases.CreateTaskUseCase;
import com.akademiaplus.task.usecases.DeleteTaskUseCase;
import com.akademiaplus.task.usecases.GetTaskByIdUseCase;
import com.akademiaplus.task.usecases.ListTasksUseCase;
import com.akademiaplus.task.usecases.UpdateTaskUseCase;
import openapi.akademiaplus.domain.task.service.dto.CompleteTaskResponseDTO;
import openapi.akademiaplus.domain.task.service.dto.CreateTaskRequestDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskListResponseDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TaskController")
@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock private CreateTaskUseCase createTaskUseCase;
    @Mock private GetTaskByIdUseCase getTaskByIdUseCase;
    @Mock private ListTasksUseCase listTasksUseCase;
    @Mock private UpdateTaskUseCase updateTaskUseCase;
    @Mock private CompleteTaskUseCase completeTaskUseCase;
    @Mock private DeleteTaskUseCase deleteTaskUseCase;

    private TaskController controller;

    private static final Long TASK_ID = 100L;
    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        controller = new TaskController(
                createTaskUseCase, getTaskByIdUseCase, listTasksUseCase,
                updateTaskUseCase, completeTaskUseCase, deleteTaskUseCase);
    }

    private void setSecurityContext() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", "pass", Collections.emptyList());
        auth.setDetails(Map.of(TaskController.CLAIM_USER_ID, USER_ID));
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Nested
    @DisplayName("Create Task")
    class CreateTask {

        @Test
        @DisplayName("Should return 201 when task created")
        void shouldReturn201_whenTaskCreated() {
            // Given
            setSecurityContext();
            CreateTaskRequestDTO dto = new CreateTaskRequestDTO();
            TaskDTO taskDto = new TaskDTO();
            taskDto.setId(TASK_ID);
            when(createTaskUseCase.create(dto, USER_ID)).thenReturn(taskDto);

            // When
            ResponseEntity<TaskDTO> response = controller.createTask(dto);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getId()).isEqualTo(TASK_ID);
            verify(createTaskUseCase, times(1)).create(dto, USER_ID);
        }
    }

    @Nested
    @DisplayName("Get Task")
    class GetTask {

        @Test
        @DisplayName("Should return 200 with task")
        void shouldReturn200_whenTaskFound() {
            // Given
            TaskDTO taskDto = new TaskDTO();
            taskDto.setId(TASK_ID);
            when(getTaskByIdUseCase.get(TASK_ID)).thenReturn(taskDto);

            // When
            ResponseEntity<TaskDTO> response = controller.getTaskById(TASK_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getId()).isEqualTo(TASK_ID);
        }
    }

    @Nested
    @DisplayName("List Tasks")
    class ListTasks {

        @Test
        @DisplayName("Should delegate filters to use case")
        void shouldDelegateFilters_whenListing() {
            // Given
            TaskListResponseDTO listDto = new TaskListResponseDTO();
            when(listTasksUseCase.list(TaskStatusDTO.PENDING, null, null, null, 0, 20)).thenReturn(listDto);

            // When
            ResponseEntity<TaskListResponseDTO> response = controller.listTasks(
                    TaskStatusDTO.PENDING, null, null, null, 0, 20);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(listTasksUseCase, times(1)).list(TaskStatusDTO.PENDING, null, null, null, 0, 20);
        }
    }

    @Nested
    @DisplayName("Update Task")
    class UpdateTask {

        @Test
        @DisplayName("Should return 200 with updated task")
        void shouldReturn200_whenTaskUpdated() {
            // Given
            UpdateTaskRequestDTO dto = new UpdateTaskRequestDTO();
            TaskDTO taskDto = new TaskDTO();
            taskDto.setId(TASK_ID);
            when(updateTaskUseCase.update(TASK_ID, dto)).thenReturn(taskDto);

            // When
            ResponseEntity<TaskDTO> response = controller.updateTask(TASK_ID, dto);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("Complete Task")
    class CompleteTask {

        @Test
        @DisplayName("Should return 200 with completion response")
        void shouldReturn200_whenTaskCompleted() {
            // Given
            CompleteTaskResponseDTO responseDto = new CompleteTaskResponseDTO();
            responseDto.setTaskId(TASK_ID);
            when(completeTaskUseCase.complete(TASK_ID)).thenReturn(responseDto);

            // When
            ResponseEntity<CompleteTaskResponseDTO> response = controller.markTaskComplete(TASK_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getTaskId()).isEqualTo(TASK_ID);
        }
    }

    @Nested
    @DisplayName("Delete Task")
    class DeleteTask {

        @Test
        @DisplayName("Should return 204 when task deleted")
        void shouldReturn204_whenTaskDeleted() {
            // When
            ResponseEntity<Void> response = controller.deleteTask(TASK_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(deleteTaskUseCase, times(1)).delete(TASK_ID);
        }
    }
}
