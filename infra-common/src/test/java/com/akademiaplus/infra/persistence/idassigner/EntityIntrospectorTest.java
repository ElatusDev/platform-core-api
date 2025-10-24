package com.akademiaplus.infra.persistence.idassigner;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for EntityIntrospector
 */
class EntityIntrospectorTest {

    private static final String TABLE_NAME = "users";
    private static final String ID_FIELD_NAME = "userId";
    private static final String EXPECTED_GETTER_NAME = "getUserId";
    private static final String EXPECTED_SETTER_NAME = "setUserId";
    private static final int EXPECTED_SETTER_PARAM_COUNT = 1;

    private EntityIntrospector introspector;

    @BeforeEach
    void setUp() {
        introspector = new EntityIntrospector();
    }

    @Test
    void shouldFindTableNameFromEntity() {
        // Given
        Class<?> entityClass = SimpleEntity.class;

        // When
        Optional<String> tableName = introspector.findTableName(entityClass);

        // Then
        assertThat(tableName)
                .isPresent()
                .hasValue(TABLE_NAME);
    }

    @Test
    void shouldReturnEmptyWhenNoTableAnnotation() {
        // Given
        Class<?> entityClass = EntityWithoutTable.class;

        // When
        Optional<String> tableName = introspector.findTableName(entityClass);

        // Then
        assertThat(tableName).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenTableNameIsEmpty() {
        // Given
        Class<?> entityClass = EntityWithEmptyTableName.class;

        // When
        Optional<String> tableName = introspector.findTableName(entityClass);

        // Then
        assertThat(tableName).isEmpty();
    }

    @Test
    void shouldFindIdFieldInEntity() {
        // Given
        Class<?> entityClass = SimpleEntity.class;

        // When
        Optional<Field> idField = introspector.findIdField(entityClass);

        // Then
        assertThat(idField)
                .isPresent()
                .get()
                .extracting(Field::getName)
                .isEqualTo(ID_FIELD_NAME);
    }

    @Test
    void shouldReturnEmptyWhenNoIdField() {
        // Given
        Class<?> entityClass = EntityWithoutId.class;

        // When
        Optional<Field> idField = introspector.findIdField(entityClass);

        // Then
        assertThat(idField).isEmpty();
    }

    @Test
    void shouldFindStandardGetter() throws Exception {
        // Given
        Class<?> entityClass = SimpleEntity.class;
        Field idField = entityClass.getDeclaredField(ID_FIELD_NAME);

        // When
        Method getter = introspector.findGetter(entityClass, idField);

        // Then
        assertThat(getter).isNotNull();
        assertThat(getter.getName()).isEqualTo(EXPECTED_GETTER_NAME);
    }

    @Test
    void shouldThrowExceptionWhenGetterNotFound() throws Exception {
        // Given
        Class<?> entityClass = EntityWithoutGetter.class;
        Field idField = entityClass.getDeclaredField(ID_FIELD_NAME);

        // When/Then
        assertThatThrownBy(() -> introspector.findGetter(entityClass, idField))
                .isInstanceOf(NoSuchMethodException.class)
                .hasMessageContaining(formatErrorMessage(
                        EntityIntrospector.ERROR_NO_GETTER_FOUND,
                        ID_FIELD_NAME,
                        entityClass.getName(),
                        "UserId"));
    }

    @Test
    void shouldFindStandardSetter() throws Exception {
        // Given
        Class<?> entityClass = SimpleEntity.class;
        Field idField = entityClass.getDeclaredField(ID_FIELD_NAME);

        // When
        Method setter = introspector.findSetter(entityClass, idField);

        // Then
        assertThat(setter).isNotNull();
        assertThat(setter.getName()).isEqualTo(EXPECTED_SETTER_NAME);
        assertThat(setter.getParameterCount()).isEqualTo(EXPECTED_SETTER_PARAM_COUNT);
    }

    @Test
    void shouldFindSetterWithDifferentParameterType() throws Exception {
        // Given
        Class<?> entityClass = EntityWithFlexibleSetter.class;
        Field idField = entityClass.getDeclaredField(ID_FIELD_NAME);

        // When
        Method setter = introspector.findSetter(entityClass, idField);

        // Then
        assertThat(setter).isNotNull();
        assertThat(setter.getName()).isEqualTo(EXPECTED_SETTER_NAME);
        assertThat(setter.getParameterCount()).isEqualTo(EXPECTED_SETTER_PARAM_COUNT);
    }

    @Test
    void shouldThrowExceptionWhenSetterNotFound() throws Exception {
        // Given
        Class<?> entityClass = EntityWithoutSetter.class;
        Field idField = entityClass.getDeclaredField(ID_FIELD_NAME);

        // When/Then
        assertThatThrownBy(() -> introspector.findSetter(entityClass, idField))
                .isInstanceOf(NoSuchMethodException.class)
                .hasMessageContaining(formatErrorMessage(
                        EntityIntrospector.ERROR_NO_SETTER_FOUND,
                        ID_FIELD_NAME,
                        entityClass.getName(),
                        EXPECTED_SETTER_NAME,
                        "Long"));
    }

    /**
     * Helper method to format error messages the same way EntityIntrospector does
     */
    private String formatErrorMessage(String messageTemplate, String... args) {
        return String.format(messageTemplate, (Object[]) args);
    }

    // Test helper classes
    @Getter
    @Setter
    @Table(name = TABLE_NAME)
    private static class SimpleEntity {
        @Id
        private Long userId;
    }

    @Getter
    @Setter
    @Table(name = "")
    private static class EntityWithEmptyTableName {
        @Id
        private Long id;
    }

    @Getter
    @Setter
    private static class EntityWithoutTable {
        @Id
        private Long id;
    }

    @Getter
    @Setter
    @Table(name = "test")
    private static class EntityWithoutId {
        private String name;
    }

    @Setter
    @Table(name = "no_getter")
    private static class EntityWithoutGetter {
        @Id
        private Long userId;
    }

    @Getter
    @Table(name = "flexible_setter")
    private static class EntityWithFlexibleSetter {
        @Id
        private Long userId;

        // Custom setter accepts Object instead of Long - cannot use Lombok @Setter
        public void setUserId(Object userId) {
            this.userId = (Long) userId;
        }
    }

    @Getter
    @Table(name = "no_setter")
    private static class EntityWithoutSetter {
        @Id
        private Long userId;
    }
}