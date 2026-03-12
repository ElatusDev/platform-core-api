package com.akademiaplus.infra.persistence.idassigner;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for EntityIntrospector
 */
@DisplayName("EntityIntrospector Tests")
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

    @Nested
    @DisplayName("findTableName")
    class FindTableName {

        @Test
        @DisplayName("Should return table name when entity has @Table annotation")
        void shouldReturnTableName_whenEntityHasTableAnnotation() {
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
        @DisplayName("Should return empty when entity has no @Table annotation")
        void shouldReturnEmpty_whenEntityHasNoTableAnnotation() {
            // Given
            Class<?> entityClass = EntityWithoutTable.class;

            // When
            Optional<String> tableName = introspector.findTableName(entityClass);

            // Then
            assertThat(tableName).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when @Table name is empty string")
        void shouldReturnEmpty_whenTableNameIsEmptyString() {
            // Given
            Class<?> entityClass = EntityWithEmptyTableName.class;

            // When
            Optional<String> tableName = introspector.findTableName(entityClass);

            // Then
            assertThat(tableName).isEmpty();
        }
    }

    @Nested
    @DisplayName("findIdField")
    class FindIdField {

        @Test
        @DisplayName("Should find @Id field when entity has one")
        void shouldFindIdField_whenEntityHasIdAnnotation() {
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
        @DisplayName("Should return empty when entity has no @Id field")
        void shouldReturnEmpty_whenEntityHasNoIdField() {
            // Given
            Class<?> entityClass = EntityWithoutId.class;

            // When
            Optional<Field> idField = introspector.findIdField(entityClass);

            // Then
            assertThat(idField).isEmpty();
        }
    }

    @Nested
    @DisplayName("findGetter")
    class FindGetter {

        @Test
        @DisplayName("Should find standard getter method for @Id field")
        void shouldFindGetter_whenStandardGetterExists() throws Exception {
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
        @DisplayName("Should throw NoSuchMethodException when getter not found")
        void shouldThrowNoSuchMethodException_whenGetterNotFound() throws Exception {
            // Given
            Class<?> entityClass = EntityWithoutGetter.class;
            Field idField = entityClass.getDeclaredField(ID_FIELD_NAME);

            // When / Then
            assertThatThrownBy(() -> introspector.findGetter(entityClass, idField))
                    .isInstanceOf(NoSuchMethodException.class)
                    .hasMessage(String.format(
                            EntityIntrospector.ERROR_NO_GETTER_FOUND,
                            ID_FIELD_NAME,
                            entityClass.getName(),
                            "UserId"));
        }
    }

    @Nested
    @DisplayName("findSetter")
    class FindSetter {

        @Test
        @DisplayName("Should find standard setter method for @Id field")
        void shouldFindSetter_whenStandardSetterExists() throws Exception {
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
        @DisplayName("Should find setter with different parameter type")
        void shouldFindSetter_whenSetterHasDifferentParameterType() throws Exception {
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
        @DisplayName("Should throw NoSuchMethodException when setter not found")
        void shouldThrowNoSuchMethodException_whenSetterNotFound() throws Exception {
            // Given
            Class<?> entityClass = EntityWithoutSetter.class;
            Field idField = entityClass.getDeclaredField(ID_FIELD_NAME);

            // When / Then
            assertThatThrownBy(() -> introspector.findSetter(entityClass, idField))
                    .isInstanceOf(NoSuchMethodException.class)
                    .hasMessage(String.format(
                            EntityIntrospector.ERROR_NO_SETTER_FOUND,
                            ID_FIELD_NAME,
                            entityClass.getName(),
                            EXPECTED_SETTER_NAME,
                            "Long"));
        }
    }

    @Nested
    @DisplayName("hasEmbeddedId")
    class HasEmbeddedId {

        @Test
        @DisplayName("Should return false when entity has no @EmbeddedId")
        void shouldReturnFalse_whenEntityHasNoEmbeddedId() {
            // Given
            Class<?> entityClass = SimpleEntity.class;

            // When
            boolean result = introspector.hasEmbeddedId(entityClass);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hasGeneratedValue")
    class HasGeneratedValue {

        @Test
        @DisplayName("Should return false when @Id field has no @GeneratedValue")
        void shouldReturnFalse_whenIdFieldHasNoGeneratedValue() throws Exception {
            // Given
            Field idField = SimpleEntity.class.getDeclaredField(ID_FIELD_NAME);

            // When
            boolean result = introspector.hasGeneratedValue(idField);

            // Then
            assertThat(result).isFalse();
        }
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
