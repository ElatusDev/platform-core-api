/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduling for the task module's overdue job.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@EnableScheduling
public class TaskSchedulingConfiguration {
}
