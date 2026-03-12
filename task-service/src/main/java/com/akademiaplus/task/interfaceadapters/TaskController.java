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
import openapi.akademiaplus.domain.task.service.api.TasksApi;
import openapi.akademiaplus.domain.task.service.dto.CompleteTaskResponseDTO;
import openapi.akademiaplus.domain.task.service.dto.CreateTaskRequestDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskListResponseDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskPriorityDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskStatusDTO;
import openapi.akademiaplus.domain.task.service.dto.UpdateTaskRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for task management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1")
public class TaskController implements TasksApi {

    public static final String CLAIM_USER_ID = "userId";
    public static final String ERROR_USER_ID_NOT_FOUND = "User ID not found in security context";

    private final CreateTaskUseCase createTaskUseCase;
    private final GetTaskByIdUseCase getTaskByIdUseCase;
    private final ListTasksUseCase listTasksUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final CompleteTaskUseCase completeTaskUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;

    public TaskController(CreateTaskUseCase createTaskUseCase,
                          GetTaskByIdUseCase getTaskByIdUseCase,
                          ListTasksUseCase listTasksUseCase,
                          UpdateTaskUseCase updateTaskUseCase,
                          CompleteTaskUseCase completeTaskUseCase,
                          DeleteTaskUseCase deleteTaskUseCase) {
        this.createTaskUseCase = createTaskUseCase;
        this.getTaskByIdUseCase = getTaskByIdUseCase;
        this.listTasksUseCase = listTasksUseCase;
        this.updateTaskUseCase = updateTaskUseCase;
        this.completeTaskUseCase = completeTaskUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
    }

    @Override
    public ResponseEntity<TaskDTO> createTask(CreateTaskRequestDTO createTaskRequestDTO) {
        Long userId = extractAuthenticatedUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createTaskUseCase.create(createTaskRequestDTO, userId));
    }

    @Override
    public ResponseEntity<TaskDTO> getTaskById(Long taskId) {
        return ResponseEntity.ok(getTaskByIdUseCase.get(taskId));
    }

    @Override
    public ResponseEntity<TaskListResponseDTO> listTasks(TaskStatusDTO status, Long assigneeId,
                                                         TaskPriorityDTO priority, Boolean overdue,
                                                         Integer page, Integer size) {
        return ResponseEntity.ok(listTasksUseCase.list(status, assigneeId, priority, overdue, page, size));
    }

    @Override
    public ResponseEntity<TaskDTO> updateTask(Long taskId, UpdateTaskRequestDTO updateTaskRequestDTO) {
        return ResponseEntity.ok(updateTaskUseCase.update(taskId, updateTaskRequestDTO));
    }

    @Override
    public ResponseEntity<CompleteTaskResponseDTO> markTaskComplete(Long taskId) {
        return ResponseEntity.ok(completeTaskUseCase.complete(taskId));
    }

    @Override
    public ResponseEntity<Void> deleteTask(Long taskId) {
        deleteTaskUseCase.delete(taskId);
        return ResponseEntity.noContent().build();
    }

    private Long extractAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token) {
            Object details = token.getDetails();
            if (details instanceof Map<?, ?> claims) {
                Object userIdClaim = claims.get(CLAIM_USER_ID);
                if (userIdClaim != null) {
                    return Long.valueOf(userIdClaim.toString());
                }
            }
        }
        throw new IllegalStateException(ERROR_USER_ID_NOT_FOUND);
    }
}
