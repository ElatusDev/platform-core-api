/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.domain;

/**
 * Supported entity types for ETL migration.
 *
 * <p>Each type maps to a specific module and JPA entity in MariaDB.</p>
 */
public enum MigrationEntityType {
    EMPLOYEE,
    COLLABORATOR,
    ADULT_STUDENT,
    TUTOR,
    MINOR_STUDENT,
    COURSE,
    SCHEDULE,
    MEMBERSHIP,
    ENROLLMENT,
    STORE_PRODUCT
}
