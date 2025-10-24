package com.akademiaplus.infra.persistence.idassigner;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EntityMetadataResolver
 */
@ExtendWith(MockitoExtension.class)
class EntityMetadataResolverTest {

    private static final String TABLE_NAME = "users";
    private static final String ANOTHER_TABLE_NAME = "another_table";
    private static final String ID_FIELD_NAME = "userId";
    private static final String ANOTHER_ID_FIELD_NAME = "id";
    private static final int EXPECTED_SINGLE_CACHE_SIZE = 1;
    private static final int EXPECTED_DOUBLE_CACHE_SIZE = 2;
    private static final int EXPECTED_SINGLE_INVOCATION = 1;

    @Mock
    private EntityIntrospector introspector;

    private EntityMetadataResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EntityMetadataResolver(introspector);
    }

    @Test
    void shouldResolveAndCacheValidEntityMetadata() throws Exception {
        // Given
        Class<?> entityClass = ValidEntity.class;
        Field idField = entityClass.getDeclaredField(ID_FIELD_NAME);
        Method getter = entityClass.getMethod("getUserId");
        Method setter = entityClass.getMethod("setUserId", Long.class);

        when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
        when(introspector.findIdField(entityClass)).thenReturn(Optional.of(idField));
        when(introspector.findGetter(entityClass, idField)).thenReturn(getter);
        when(introspector.findSetter(entityClass, idField)).thenReturn(setter);

        // When
        EntityMetadata metadata = resolver.resolve(entityClass);

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.isSkip()).isFalse();
        assertThat(metadata.getTableName()).isEqualTo(TABLE_NAME);
        assertThat(metadata.getIdFieldName()).isEqualTo(ID_FIELD_NAME);
        assertThat(metadata.getGetter()).isEqualTo(getter);
        assertThat(metadata.getSetter()).isEqualTo(setter);
    }

    @Test
    void shouldCacheMetadataAndNotCallIntrospectorTwice() throws NoSuchMethodException {
        // Given
        Class<?> entityClass = ValidEntity.class;
        Field mockField = mock(Field.class);
        when(mockField.getName()).thenReturn(ID_FIELD_NAME);

        when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
        when(introspector.findIdField(entityClass)).thenReturn(Optional.of(mockField));
        when(introspector.findGetter(entityClass, mockField)).thenReturn(mock(Method.class));
        when(introspector.findSetter(entityClass, mockField)).thenReturn(mock(Method.class));

        // When
        EntityMetadata firstCall = resolver.resolve(entityClass);
        EntityMetadata secondCall = resolver.resolve(entityClass);

        // Then
        assertThat(firstCall).isSameAs(secondCall); // Same instance from cache
        verify(introspector, times(EXPECTED_SINGLE_INVOCATION)).findTableName(entityClass); // Only called once
    }

    @Test
    void shouldReturnSkipMetadataWhenNoTableAnnotation() {
        // Given
        Class<?> entityClass = ValidEntity.class;
        when(introspector.findTableName(entityClass)).thenReturn(Optional.empty());

        // When
        EntityMetadata metadata = resolver.resolve(entityClass);

        // Then
        assertThat(metadata.isSkip()).isTrue();
    }

    @Test
    void shouldReturnSkipMetadataWhenNoIdField() {
        // Given
        Class<?> entityClass = ValidEntity.class;
        when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
        when(introspector.findIdField(entityClass)).thenReturn(Optional.empty());

        // When
        EntityMetadata metadata = resolver.resolve(entityClass);

        // Then
        assertThat(metadata.isSkip()).isTrue();
    }

    @Test
    void shouldReturnSkipMetadataWhenGetterThrowsException() throws Exception {
        // Given
        Class<?> entityClass = ValidEntity.class;
        Field idField = mock(Field.class);

        when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
        when(introspector.findIdField(entityClass)).thenReturn(Optional.of(idField));
        when(introspector.findGetter(entityClass, idField))
                .thenThrow(new NoSuchMethodException("Getter not found"));

        // When
        EntityMetadata metadata = resolver.resolve(entityClass);

        // Then
        assertThat(metadata.isSkip()).isTrue();
    }

    @Test
    void shouldReturnSkipMetadataWhenSetterThrowsException() throws Exception {
        // Given
        Class<?> entityClass = ValidEntity.class;
        Field idField = mock(Field.class);
        Method getter = mock(Method.class);

        when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
        when(introspector.findIdField(entityClass)).thenReturn(Optional.of(idField));
        when(introspector.findGetter(entityClass, idField)).thenReturn(getter);
        when(introspector.findSetter(entityClass, idField))
                .thenThrow(new NoSuchMethodException("Setter not found"));

        // When
        EntityMetadata metadata = resolver.resolve(entityClass);

        // Then
        assertThat(metadata.isSkip()).isTrue();
    }

    @Test
    void shouldClearCache() throws NoSuchMethodException {
        // Given
        Class<?> entityClass = ValidEntity.class;
        Field mockField = mock(Field.class);
        when(mockField.getName()).thenReturn(ID_FIELD_NAME);

        when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
        when(introspector.findIdField(entityClass)).thenReturn(Optional.of(mockField));
        when(introspector.findGetter(entityClass, mockField)).thenReturn(mock(Method.class));
        when(introspector.findSetter(entityClass, mockField)).thenReturn(mock(Method.class));

        resolver.resolve(entityClass);
        int initialCacheSize = resolver.getCacheSize();

        // When
        resolver.clearCache();

        // Then
        assertThat(resolver.getCacheSize()).isZero();
        assertThat(initialCacheSize).isEqualTo(EXPECTED_SINGLE_CACHE_SIZE);
    }

    @Test
    void shouldReturnCorrectCacheSize() throws NoSuchMethodException {
        // Given
        Field mockField1 = mock(Field.class);
        when(mockField1.getName()).thenReturn(ID_FIELD_NAME);
        Field mockField2 = mock(Field.class);
        when(mockField2.getName()).thenReturn(ANOTHER_ID_FIELD_NAME);

        when(introspector.findTableName(ValidEntity.class)).thenReturn(Optional.of(TABLE_NAME));
        when(introspector.findIdField(ValidEntity.class)).thenReturn(Optional.of(mockField1));
        when(introspector.findGetter(ValidEntity.class, mockField1)).thenReturn(mock(Method.class));
        when(introspector.findSetter(ValidEntity.class, mockField1)).thenReturn(mock(Method.class));

        when(introspector.findTableName(AnotherEntity.class)).thenReturn(Optional.of(ANOTHER_TABLE_NAME));
        when(introspector.findIdField(AnotherEntity.class)).thenReturn(Optional.of(mockField2));
        when(introspector.findGetter(AnotherEntity.class, mockField2)).thenReturn(mock(Method.class));
        when(introspector.findSetter(AnotherEntity.class, mockField2)).thenReturn(mock(Method.class));

        // When
        resolver.resolve(ValidEntity.class);
        resolver.resolve(AnotherEntity.class);
        int cacheSize = resolver.getCacheSize();

        // Then
        assertThat(cacheSize).isEqualTo(EXPECTED_DOUBLE_CACHE_SIZE);
    }

    @Test
    void shouldHandleConcurrentResolveCallsForSameEntity() throws InterruptedException, NoSuchMethodException {
        // Given
        Class<?> entityClass = ValidEntity.class;
        Field mockField = mock(Field.class);
        when(mockField.getName()).thenReturn(ID_FIELD_NAME);

        when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
        when(introspector.findIdField(entityClass)).thenReturn(Optional.of(mockField));
        when(introspector.findGetter(entityClass, mockField)).thenReturn(mock(Method.class));
        when(introspector.findSetter(entityClass, mockField)).thenReturn(mock(Method.class));

        // When - simulate concurrent access
        Thread thread1 = new Thread(() -> resolver.resolve(entityClass));
        Thread thread2 = new Thread(() -> resolver.resolve(entityClass));

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Then
        assertThat(resolver.getCacheSize()).isEqualTo(EXPECTED_SINGLE_CACHE_SIZE);
        verify(introspector, times(EXPECTED_SINGLE_INVOCATION)).findTableName(entityClass); // Called only once despite concurrent access
    }

    // Test helper classes
    @Getter
    @Setter
    @Table(name = TABLE_NAME)
    private static class ValidEntity {
        @Id
        private Long userId;
    }

    @Getter
    @Setter
    @Table(name = ANOTHER_TABLE_NAME)
    private static class AnotherEntity {
        @Id
        private Long id;
    }
}