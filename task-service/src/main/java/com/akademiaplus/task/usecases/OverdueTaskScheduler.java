/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.usecases;

import com.akademiaplus.task.interfaceadapters.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Scheduled job that marks overdue tasks across all tenants.
 * Runs daily at 1:00 AM.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Slf4j
@Service
public class OverdueTaskScheduler {

    public static final String LOG_OVERDUE_TASKS_MARKED = "Marked {} tasks as OVERDUE";

    private final TaskRepository taskRepository;

    public OverdueTaskScheduler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void markOverdueTasks() {
        int count = taskRepository.markOverdueTasks(LocalDate.now());
        log.info(LOG_OVERDUE_TASKS_MARKED, count);
    }
}
