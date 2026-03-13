/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.domain.exception;

/**
 * Thrown when a schedule is already assigned to another course during course update.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class ScheduleConflictException extends RuntimeException {

    /** Error message for schedule conflict. */
    public static final String ERROR_MESSAGE = "Schedule(s) already assigned to another course";

    /**
     * Creates a new exception with the conflicting schedule IDs appended.
     *
     * @param conflictingScheduleIds comma-separated IDs of conflicting schedules
     */
    public ScheduleConflictException(String conflictingScheduleIds) {
        super(ERROR_MESSAGE + ": " + conflictingScheduleIds);
    }
}
