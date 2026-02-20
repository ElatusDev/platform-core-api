/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.usecases;

import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Shared soft-delete execution logic for all delete use cases.
 * <p>
 * Encapsulates the find-or-throw → try-delete → catch-constraint pattern
 * that is identical across all 19 delete use cases. Individual use cases
 * compose this utility rather than inheriting from it.
 * <p>
 * For entities with pre-delete business rules (e.g., Tutor with active
 * MinorStudents), the use case performs validation before calling
 * {@link #executeDelete}.
 *
 * @author ElatusDev
 * @since 1.0
 */
public final class DeleteUseCaseSupport {

    private DeleteUseCaseSupport() {
    }

    /**
     * Executes soft-delete for a tenant-scoped entity.
     * <p>
     * Flow: findById → present? delete : throw 404
     *       delete succeeds? return : catch constraint → throw 409
     *
     * @param repository  the tenant-scoped repository for the entity
     * @param compositeId the composite key (tenantId + entityId)
     * @param entityType  message key from {@link com.akademiaplus.utilities.EntityType}
     * @param entityId    the entity ID as display string
     * @param <T>         the entity type
     * @param <ID>        the composite ID type
     * @throws EntityNotFoundException if no entity exists with the given composite key
     * @throws EntityDeletionNotAllowedException if a database constraint prevents deletion
     */
    public static <T, ID> void executeDelete(
            TenantScopedRepository<T, ID> repository,
            ID compositeId,
            String entityType,
            String entityId) {

        T entity = repository.findById(compositeId)
                .orElseThrow(() -> new EntityNotFoundException(entityType, entityId));

        try {
            repository.delete(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new EntityDeletionNotAllowedException(entityType, entityId, ex);
        }
    }

    /**
     * Finds an entity by composite key or throws 404.
     * <p>
     * Use this when pre-delete validation is needed before calling delete.
     * The use case performs business checks on the returned entity, then
     * calls {@link #executeDelete} or {@code repository.delete()} directly.
     *
     * @param repository  the tenant-scoped repository
     * @param compositeId the composite key
     * @param entityType  message key from {@link com.akademiaplus.utilities.EntityType}
     * @param entityId    the entity ID as display string
     * @param <T>         the entity type
     * @param <ID>        the composite ID type
     * @return the found entity, never null
     * @throws EntityNotFoundException if no entity exists with the given composite key
     */
    public static <T, ID> T findOrThrow(
            TenantScopedRepository<T, ID> repository,
            ID compositeId,
            String entityType,
            String entityId) {

        return repository.findById(compositeId)
                .orElseThrow(() -> new EntityNotFoundException(entityType, entityId));
    }
}
