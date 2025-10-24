package com.akademiaplus.infra.persistence.idassigner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves and caches entity metadata using EntityIntrospector
 * Provides thread-safe caching of reflection-based entity information
 */
@Slf4j
@Component
public class EntityMetadataResolver {

    // Log messages (public for testing)
    public static final String WARN_NO_TABLE_ANNOTATION = "No @Table annotation found for entity: {}";
    public static final String WARN_NO_ID_FIELD = "No @Id field found for entity: {}";
    public static final String ERROR_FAILED_TO_BUILD_METADATA = "Failed to build metadata for entity: {}";

    // Log messages (private - internal only)
    private static final String INFO_CACHE_CLEARED = "Entity metadata cache cleared";

    private final EntityIntrospector introspector;

    // Cache for entity metadata to avoid repeated reflection
    private final Map<Class<?>, EntityMetadata> metadataCache = new ConcurrentHashMap<>();

    public EntityMetadataResolver(EntityIntrospector introspector) {
        this.introspector = introspector;
    }

    /**
     * Get or compute entity metadata with caching
     */
    public EntityMetadata resolve(Class<?> entityClass) {
        return metadataCache.computeIfAbsent(entityClass, this::buildMetadata);
    }

    /**
     * Build metadata for an entity class (called once per class)
     */
    private EntityMetadata buildMetadata(Class<?> entityClass) {
        try {
            // Find table name
            String tableName = introspector.findTableName(entityClass)
                    .orElseGet(() -> {
                        log.warn(WARN_NO_TABLE_ANNOTATION, entityClass.getName());
                        return null;
                    });

            if (tableName == null) {
                return EntityMetadata.skip();
            }

            // Find @Id field
            Field idField = introspector.findIdField(entityClass)
                    .orElseGet(() -> {
                        log.warn(WARN_NO_ID_FIELD, entityClass.getName());
                        return null;
                    });

            if (idField == null) {
                return EntityMetadata.skip();
            }

            // Find getter and setter
            Method getter = introspector.findGetter(entityClass, idField);
            Method setter = introspector.findSetter(entityClass, idField);

            return EntityMetadata.of(tableName, idField.getName(), getter, setter);

        } catch (Exception e) {
            log.error(ERROR_FAILED_TO_BUILD_METADATA, entityClass.getName(), e);
            return EntityMetadata.skip();
        }
    }

    /**
     * Clear metadata cache (useful for testing or hot reload scenarios)
     */
    public void clearCache() {
        metadataCache.clear();
        log.info(INFO_CACHE_CLEARED);
    }

    /**
     * Get cache statistics (useful for monitoring)
     */
    public int getCacheSize() {
        return metadataCache.size();
    }
}