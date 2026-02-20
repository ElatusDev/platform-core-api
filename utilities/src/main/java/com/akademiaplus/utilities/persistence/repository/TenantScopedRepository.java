/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.persistence.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Restricted base repository for tenant-scoped entities that use composite keys.
 *
 * <p>Extends Spring Data's {@link Repository} marker interface instead of
 * {@code JpaRepository} so that <strong>only</strong> the methods declared here
 * are visible to consumers. This prevents accidental use of ID-based operations
 * that accept a single field rather than the full composite key.</p>
 *
 * <p>Every tenant-scoped repository should extend this interface with the
 * entity's {@code @IdClass} composite key as the {@code ID} type parameter.</p>
 *
 * @param <T>  the entity type
 * @param <ID> the composite key type (must match the entity's {@code @IdClass})
 * @see org.springframework.data.repository.Repository
 */
@NoRepositoryBean
public interface TenantScopedRepository<T, ID> extends Repository<T, ID> {

    /**
     * Retrieves an entity by its composite identifier.
     *
     * @param id the composite key (never {@code null})
     * @return an {@link Optional} containing the entity, or empty if not found
     */
    Optional<T> findById(ID id);

    /**
     * Returns all entities matching the given composite identifiers.
     *
     * @param ids the composite keys to look up (never {@code null})
     * @return the matching entities (never {@code null})
     */
    List<T> findAllById(Iterable<ID> ids);

    /**
     * Returns all entities of this type.
     *
     * @return all entities (never {@code null})
     */
    List<T> findAll();

    /**
     * Saves a given entity. Use the returned instance for further operations
     * as the save operation might have changed the entity instance completely.
     *
     * @param entity the entity to save (never {@code null})
     * @param <S>    the entity subtype
     * @return the saved entity (never {@code null})
     */
    <S extends T> S save(S entity);

    /**
     * Saves all given entities.
     *
     * @param entities the entities to save (never {@code null})
     * @param <S>      the entity subtype
     * @return the saved entities (never {@code null})
     */
    <S extends T> List<S> saveAll(Iterable<S> entities);

    /**
     * Saves an entity and flushes changes instantly.
     *
     * @param entity the entity to save (never {@code null})
     * @param <S>    the entity subtype
     * @return the saved entity (never {@code null})
     */
    <S extends T> S saveAndFlush(S entity);

    /**
     * Deletes a given entity (soft-delete via {@code @SQLDelete}).
     *
     * @param entity the entity to delete (never {@code null})
     */
    void delete(T entity);

    /**
     * Deletes all entities in a batch call.
     * Used by the mock-data cleanup process to truncate tables.
     */
    void deleteAllInBatch();

    /**
     * Flushes all pending changes to the database.
     */
    void flush();
}
