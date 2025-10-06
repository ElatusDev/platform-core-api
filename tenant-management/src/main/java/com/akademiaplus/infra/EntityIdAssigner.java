package com.akademiaplus.infra;

import com.akademiaplus.utilities.idgeneration.IDGeneratorService;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.event.spi.PreInsertEvent;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

// Extracted ID assignment logic with tenant support
@Component
public class EntityIdAssigner {

    private final IDGeneratorService idGeneratorService;
    private final TenantContextHolder tenantContextHolder;

    public EntityIdAssigner(IDGeneratorService idGeneratorService,
                            TenantContextHolder tenantContextHolder) {
        this.idGeneratorService = idGeneratorService;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Assigns an ID to the entity if needed
     */
    public void assignIdIfNeeded(Object entity, PreInsertEvent event) throws Exception {
        Class<?> entityClass = entity.getClass();

        // 1. Get table name from @Table annotation
        String tableName = getTableName(entityClass);

        // 2. Find the @Id field
        Field idField = getIdField(entityClass);

        if (idField != null && tableName != null) {
            // Get current ID using getter method
            Method getter = findGetter(entityClass, idField);
            Object currentId = getter.invoke(entity);

            // 3. Generate and assign ID if not already set
            if (shouldGenerateId(currentId)) {
                // Get tenantId from TenantContextHolder
                Integer tenantId = tenantContextHolder.getTenantId().orElse(null);

                // Generate ID with tenantId
                Object generatedId = idGeneratorService.generateId(tableName, tenantId);
                assignIdToEntity(entity, entityClass, idField, generatedId);
                updateEventState(event, idField.getName(), generatedId);
            }
        }
    }

    /**
     * Extract table name from @Table annotation
     */
    private String getTableName(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);

        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }

        return null;
    }

    /**
     * Find the field annotated with @Id
     */
    private Field getIdField(Class<?> entityClass) {
        // Check current class
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }

        return null;
    }

    /**
     * Check if ID should be generated
     */
    private boolean shouldGenerateId(Object currentId) {
        return currentId == null;
    }

    /**
     * Assign the generated ID to the entity using setter method
     */
    private void assignIdToEntity(Object entity, Class<?> entityClass, Field idField, Object generatedId) throws Exception {
        Method setter = findSetter(entityClass, idField, generatedId.getClass());
        setter.invoke(entity, generatedId);
    }

    /**
     * Find the getter method for the ID field
     */
    private Method findGetter(Class<?> entityClass, Field field) throws NoSuchMethodException {
        String fieldName = field.getName();
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        // Try standard getter
        try {
            return entityClass.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            // Try boolean getter if field is boolean
            if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                String booleanGetterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                return entityClass.getMethod(booleanGetterName);
            }
            throw e;
        }
    }

    /**
     * Find the setter method for the ID field
     */
    private Method findSetter(Class<?> entityClass, Field field, Class<?> parameterType) throws NoSuchMethodException {
        String fieldName = field.getName();
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        // Try with the exact parameter type
        try {
            return entityClass.getMethod(setterName, parameterType);
        } catch (NoSuchMethodException e) {
            // Try with the field's declared type
            return entityClass.getMethod(setterName, field.getType());
        }
    }

    /**
     * Update the Hibernate event state with the new ID
     */
    private void updateEventState(PreInsertEvent event, String idFieldName, Object generatedId) {
        String[] propertyNames = event.getPersister().getPropertyNames();
        Object[] state = event.getState();

        for (int i = 0; i < propertyNames.length; i++) {
            if (propertyNames[i].equals(idFieldName)) {
                state[i] = generatedId;
                break;
            }
        }
    }
}
