/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for {@link TaskDataModel}.
 * Combines tenant isolation with task-specific identity.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskId implements Serializable {

    /** The tenant that owns this task. */
    private Long tenantId;

    /** The unique task identifier within the tenant. */
    private Long taskId;
}
