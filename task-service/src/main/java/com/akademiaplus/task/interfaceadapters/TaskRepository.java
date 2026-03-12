/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.interfaceadapters;

import com.akademiaplus.task.TaskDataModel;
import com.akademiaplus.task.TaskId;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for {@link TaskDataModel} with tenant-scoped composite keys.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Repository
public interface TaskRepository extends TenantScopedRepository<TaskDataModel, TaskId> {

    Optional<TaskDataModel> findByTenantIdAndTaskId(Long tenantId, Long taskId);

    Page<TaskDataModel> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<TaskDataModel> findAllByTenantIdAndStatus(Long tenantId, String status, Pageable pageable);

    Page<TaskDataModel> findAllByTenantIdAndAssigneeId(Long tenantId, Long assigneeId, Pageable pageable);

    Page<TaskDataModel> findAllByTenantIdAndPriority(Long tenantId, String priority, Pageable pageable);

    @Query("SELECT t FROM TaskDataModel t WHERE t.tenantId = :tenantId "
         + "AND t.status <> 'COMPLETED' AND t.dueDate < CURRENT_DATE")
    Page<TaskDataModel> findOverdueTasks(@Param("tenantId") Long tenantId, Pageable pageable);

    @Modifying
    @Query("UPDATE TaskDataModel t SET t.status = 'OVERDUE' "
         + "WHERE t.dueDate < :now AND t.status NOT IN ('COMPLETED', 'OVERDUE') "
         + "AND t.deletedAt IS NULL")
    int markOverdueTasks(@Param("now") LocalDate now);
}
