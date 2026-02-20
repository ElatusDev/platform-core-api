/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.infra.persistence.listeners;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hibernate PreInsert event listener that automatically populates writable Long FK fields
 * from their corresponding relationship objects.
 * <p>
 * In a multi-tenant composite key model, relationship {@code @JoinColumn} annotations
 * must use {@code insertable=false, updatable=false} to avoid conflicts with the shared
 * tenant_id column. A separate writable {@code @Column} Long field provides the insertable
 * FK value. This listener bridges the gap: when a relationship object already has its ID
 * assigned (e.g., after cascade persist), the listener copies that ID into the writable
 * Long FK field so Hibernate includes it in the INSERT statement.
 * <p>
 * This listener must be registered AFTER {@code IdAssignationPreInsertEventListener}
 * so that cascaded child entities already have their IDs assigned.
 */
@Slf4j
@Component
public class ForeignKeyWiringPreInsertEventListener implements PreInsertEventListener {

    /**
     * Log message when a FK value is auto-wired from a relationship object.
     */
    private static final String DEBUG_FK_WIRED =
            "Auto-wired FK field '{}' = {} on entity {} from relationship '{}'";

    /**
     * Cached FK wiring metadata per entity class to avoid repeated reflection.
     */
    private final Map<Class<?>, List<FkWiringEntry>> wiringCache = new ConcurrentHashMap<>();

    /**
     * Called before an entity is inserted. Populates any null writable Long FK fields
     * from the corresponding relationship object's ID.
     *
     * @param event the pre-insert event
     * @return false to allow the insert to proceed
     */
    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        Object entity = event.getEntity();
        List<FkWiringEntry> entries = wiringCache.computeIfAbsent(
                entity.getClass(), this::buildWiringEntries);

        if (entries.isEmpty()) {
            return false;
        }

        String[] propertyNames = event.getPersister().getPropertyNames();
        Object[] state = event.getState();

        for (FkWiringEntry entry : entries) {
            try {
                Object currentFkValue = entry.fkGetter().invoke(entity);
                if (currentFkValue != null) {
                    continue;
                }

                Object relatedEntity = entry.relationshipGetter().invoke(entity);
                if (relatedEntity == null) {
                    continue;
                }

                Object relatedId = entry.relatedIdGetter().invoke(relatedEntity);
                if (relatedId == null) {
                    continue;
                }

                entry.fkSetter().invoke(entity, relatedId);
                updatePropertyInState(propertyNames, state, entry.fkFieldName(), relatedId);

                log.debug(DEBUG_FK_WIRED, entry.fkFieldName(), relatedId,
                        entity.getClass().getSimpleName(), entry.relationshipFieldName());

            } catch (Exception e) {
                log.warn("Failed to auto-wire FK field '{}' on entity {}: {}",
                        entry.fkFieldName(), entity.getClass().getSimpleName(), e.getMessage());
            }
        }

        return false;
    }

    /**
     * Builds the FK wiring metadata for an entity class by scanning its fields
     * for writable Long FK columns that have matching read-only relationship mappings.
     *
     * @param entityClass the entity class to inspect
     * @return list of FK wiring entries, empty if none found
     */
    private List<FkWiringEntry> buildWiringEntries(Class<?> entityClass) {
        List<FkWiringEntry> entries = new ArrayList<>();
        List<Field> allFields = getAllFields(entityClass);

        Map<String, Field> columnFieldsByName = new java.util.HashMap<>();
        Map<String, Field> relationshipFields = new java.util.HashMap<>();

        for (Field field : allFields) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && field.getType() == Long.class) {
                columnFieldsByName.put(column.name(), field);
            }

            if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
                JoinColumn[] joinColumns = field.getAnnotationsByType(JoinColumn.class);
                for (JoinColumn jc : joinColumns) {
                    if (!jc.insertable() && !"tenant_id".equals(jc.name())) {
                        relationshipFields.put(jc.name(), field);
                    }
                }
            }
        }

        for (Map.Entry<String, Field> relEntry : relationshipFields.entrySet()) {
            String columnName = relEntry.getKey();
            Field relField = relEntry.getValue();
            Field fkField = columnFieldsByName.get(columnName);

            if (fkField == null) {
                continue;
            }

            try {
                Method fkGetter = findGetter(entityClass, fkField);
                Method fkSetter = findSetter(entityClass, fkField);
                Method relGetter = findGetter(entityClass, relField);

                String referencedColumnName = findReferencedColumnName(relField, columnName);
                Method relatedIdGetter = findGetterByColumnName(
                        relField.getType(), referencedColumnName);

                if (relatedIdGetter != null) {
                    entries.add(new FkWiringEntry(
                            fkField.getName(), relField.getName(),
                            fkGetter, fkSetter, relGetter, relatedIdGetter));
                }
            } catch (Exception e) {
                log.warn("Could not build FK wiring for field '{}' on entity {}: {}",
                        fkField.getName(), entityClass.getSimpleName(), e.getMessage());
            }
        }

        return entries;
    }

    /**
     * Finds the referencedColumnName from a JoinColumn annotation for a given column name.
     */
    private String findReferencedColumnName(Field relField, String columnName) {
        JoinColumn[] joinColumns = relField.getAnnotationsByType(JoinColumn.class);
        for (JoinColumn jc : joinColumns) {
            if (jc.name().equals(columnName)) {
                return jc.referencedColumnName().isEmpty() ? columnName : jc.referencedColumnName();
            }
        }
        return columnName;
    }

    /**
     * Finds a getter method on the target class that maps to the specified column name.
     */
    private Method findGetterByColumnName(Class<?> targetClass, String columnName) {
        List<Field> fields = getAllFields(targetClass);
        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.name().equals(columnName)) {
                try {
                    return findGetter(targetClass, field);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Gets all fields from a class hierarchy (including inherited fields from superclasses).
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * Finds a getter method for the given field, trying both standard and boolean naming.
     */
    private Method findGetter(Class<?> clazz, Field field) throws NoSuchMethodException {
        String capitalized = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        try {
            return clazz.getMethod("get" + capitalized);
        } catch (NoSuchMethodException e) {
            return clazz.getMethod("is" + capitalized);
        }
    }

    /**
     * Finds a setter method for the given field.
     */
    private Method findSetter(Class<?> clazz, Field field) throws NoSuchMethodException {
        String capitalized = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        return clazz.getMethod("set" + capitalized, field.getType());
    }

    /**
     * Updates the Hibernate property state array for the given property name.
     */
    private void updatePropertyInState(String[] propertyNames, Object[] state,
                                       String propertyName, Object newValue) {
        for (int i = 0; i < propertyNames.length; i++) {
            if (propertyNames[i].equals(propertyName)) {
                state[i] = newValue;
                return;
            }
        }
    }

    /**
     * Immutable record holding the reflection metadata needed to wire a single FK field.
     *
     * @param fkFieldName           name of the writable Long FK field on the entity
     * @param relationshipFieldName name of the relationship field on the entity
     * @param fkGetter              getter for the Long FK field
     * @param fkSetter              setter for the Long FK field
     * @param relationshipGetter    getter for the relationship object
     * @param relatedIdGetter       getter for the ID on the related entity
     */
    private record FkWiringEntry(
            String fkFieldName,
            String relationshipFieldName,
            Method fkGetter,
            Method fkSetter,
            Method relationshipGetter,
            Method relatedIdGetter) {
    }
}
