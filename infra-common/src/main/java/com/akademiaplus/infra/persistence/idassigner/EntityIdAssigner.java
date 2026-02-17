package com.akademiaplus.infra.persistence.idassigner;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.infra.persistence.exceptions.IdAssignmentException;
import com.akademiaplus.utilities.idgeneration.IDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PreInsertEvent;
import org.springframework.stereotype.Component;

/**
 * Orchestrates ID assignment for entities by delegating to specialized components
 * <p>
 * Responsibilities:
 * - Coordinates the ID assignment workflow
 * - Delegates to specialized components for specific concerns
 */
@Slf4j
@Component
public class EntityIdAssigner {

    // Error messages
    public static final String ERROR_MISSING_ANNOTATION = "missing id or table annotation in model class!";
    public static final String ERROR_CANNOT_GENERATE_ID = "Cannot generate ID for entity {}";
    public static final String ERROR_FAILED_TO_ASSIGN_ID = "Failed to assign ID for entity: {}";

    // Warning messages
    public static final String WARNING_PRESET_ID =
            "SECURITY/BUG WARNING: Entity {} has pre-set ID value: {}. " +
                    "IDs should ONLY be assigned by EntityIdAssigner. " +
                    "This may indicate a security issue or bug. " +
                    "The value will be OVERRIDDEN with a generated ID. " +
                    "Entity class: {}, Tenant: {}";

    private final IDGenerator idGenerator;
    private final TenantContextHolder tenantContextHolder;
    private final EntityMetadataResolver metadataResolver;
    private final IdGenerationStrategy idGenerationStrategy;
    private final HibernateStateUpdater hibernateStateUpdater;

    public EntityIdAssigner(IDGenerator idGenerator,
                            TenantContextHolder tenantContextHolder,
                            EntityMetadataResolver metadataResolver,
                            IdGenerationStrategy idGenerationStrategy,
                            HibernateStateUpdater hibernateStateUpdater) {
        this.idGenerator = idGenerator;
        this.tenantContextHolder = tenantContextHolder;
        this.metadataResolver = metadataResolver;
        this.idGenerationStrategy = idGenerationStrategy;
        this.hibernateStateUpdater = hibernateStateUpdater;
    }

    /**
     * Assigns an ID to the entity, always overriding any pre-existing value
     * <p>
     * This is the ONLY place where entity IDs should be assigned.
     * If an ID is already present, it will be overridden and a warning logged
     * as this may indicate a security issue or bug.
     *
     * @param entity the entity that needs an ID
     * @param event the Hibernate PreInsertEvent
     * @throws IdAssignmentException if ID assignment fails
     */
    public void assignIdIfNeeded(Object entity, PreInsertEvent event) {
        try {
            Class<?> entityClass = entity.getClass();
            EntityMetadata metadata = metadataResolver.resolve(entityClass);

            if (metadata.isSkip()) {
                throw new IdAssignmentException(ERROR_MISSING_ANNOTATION);
            }

            Object currentId = metadata.getGetter().invoke(entity);

            if (!idGenerationStrategy.shouldGenerateId(currentId)) {
                log.warn(WARNING_PRESET_ID,
                        entityClass.getSimpleName(),
                        currentId,
                        entityClass.getName(),
                        tenantContextHolder.getTenantId().orElse(null));
            }

            Long tenantId = tenantContextHolder.getTenantId().orElse(null);
            Object generatedId = idGenerator.generateId(metadata.getTableName(), tenantId);
            metadata.getSetter().invoke(entity, generatedId);
            hibernateStateUpdater.updatePropertyInState(event, metadata.getIdFieldName(), generatedId);
        } catch (Exception e) {
            String entityName = entity.getClass().getSimpleName();
            log.error(ERROR_FAILED_TO_ASSIGN_ID, entityName, e);
            throw new IdAssignmentException(
                    String.format(ERROR_CANNOT_GENERATE_ID.replace("{}", "%s"), entityName), e);
        }
    }

}