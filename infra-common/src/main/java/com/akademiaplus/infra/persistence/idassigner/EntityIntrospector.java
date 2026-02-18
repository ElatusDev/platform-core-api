package com.akademiaplus.infra.persistence.idassigner;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Handles reflection-based introspection of JPA entities
 * Finds @Table annotations, @Id fields, and accessor methods
 */
@Slf4j
@Component
public class EntityIntrospector {

    // Method prefixes
    private static final String GETTER_PREFIX = "get";
    private static final String SETTER_PREFIX = "set";

    // Error messages (public for testing)
    public static final String ERROR_NO_GETTER_FOUND =
            "No getter found for field '%s' in class %s. Expected: get%s()";
    public static final String ERROR_NO_SETTER_FOUND =
            "No setter found for field '%s' in class %s. Expected: %s(%s)";

    /**
     * Find table name from @Table annotation
     *
     * @return Optional containing table name if found, empty otherwise
     */
    public Optional<String> findTableName(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);

        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return Optional.of(tableAnnotation.name());
        }

        return Optional.empty();
    }

    /**
     * Find @Id field in the entity class
     *
     * @return Optional containing the ID field if found, empty otherwise
     */
    public Optional<Field> findIdField(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                return Optional.of(field);
            }
        }

        return Optional.empty();
    }

    /**
     * Find getter method (ID fields use standard get prefix)
     *
     * @throws NoSuchMethodException if getter not found
     */
    public Method findGetter(Class<?> entityClass, Field field) throws NoSuchMethodException {
        String fieldName = field.getName();
        String capitalizedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String getterName = GETTER_PREFIX + capitalizedFieldName;

        try {
            return entityClass.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodException(
                    String.format(ERROR_NO_GETTER_FOUND,
                            fieldName,
                            entityClass.getName(),
                            capitalizedFieldName));
        }
    }

    /**
     * Find setter method with type compatibility handling
     *
     * @throws NoSuchMethodException if setter not found
     */
    public Method findSetter(Class<?> entityClass, Field field) throws NoSuchMethodException {
        String fieldName = field.getName();
        String capitalizedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String setterName = SETTER_PREFIX + capitalizedFieldName;

        // Try to find setter with exact field type
        try {
            return entityClass.getMethod(setterName, field.getType());
        } catch (NoSuchMethodException e) {
            // Try to find any setter with that name (for type flexibility)
            for (Method method : entityClass.getMethods()) {
                if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                    return method;
                }
            }

            throw new NoSuchMethodException(
                    String.format(ERROR_NO_SETTER_FOUND,
                            fieldName,
                            entityClass.getName(),
                            setterName,
                            field.getType().getSimpleName()));
        }
    }

    /**
     * Check if the entity uses @EmbeddedId (self-managed composite key)
     *
     * @return true if any field is annotated with @EmbeddedId
     */
    public boolean hasEmbeddedId(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(EmbeddedId.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the @Id field uses @GeneratedValue (DB-managed ID)
     *
     * @return true if the field is annotated with @GeneratedValue
     */
    public boolean hasGeneratedValue(Field idField) {
        return idField.isAnnotationPresent(GeneratedValue.class);
    }
}