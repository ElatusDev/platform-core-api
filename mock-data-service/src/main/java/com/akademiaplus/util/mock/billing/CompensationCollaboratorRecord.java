/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import java.time.LocalDate;

/**
 * Lightweight record representing a compensation-to-collaborator bridge table row.
 *
 * <p>Used exclusively by {@link CompensationCollaboratorFactory} for mock data generation.
 * This is NOT a JPA entity.</p>
 *
 * @param compensationId  the compensation FK
 * @param collaboratorId  the collaborator FK
 * @param assignedDate    the date the compensation was assigned to the collaborator
 */
public record CompensationCollaboratorRecord(Long compensationId, Long collaboratorId, LocalDate assignedDate) {}
