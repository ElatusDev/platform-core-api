package com.akademiaplus.infra.persistence.idassigner;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
@DisplayName("EntityMetadataResolver Tests")
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

    @Nested
    @DisplayName("Happy path resolution")
    class HappyPath {

        @Test
        @DisplayName("Should resolve and cache valid entity metadata")
        void shouldResolveAndCacheValidEntityMetadata_whenEntityHasAllAnnotations() throws Exception {
            // Given
            Class<?> entityClass = ValidEntity.class;
            Field idField = entityClass.getDeclaredField(ID_FIELD_NAME);
            Method getter = entityClass.getMethod("getUserId");
            Method setter = entityClass.getMethod("setUserId", Long.class);

            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass)).thenReturn(false);
            when(introspector.findIdField(entityClass)).thenReturn(Optional.of(idField));
            when(introspector.hasGeneratedValue(idField)).thenReturn(false);
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

            InOrder inOrder = inOrder(introspector);
            inOrder.verify(introspector, times(1)).findTableName(entityClass);
            inOrder.verify(introspector, times(1)).hasEmbeddedId(entityClass);
            inOrder.verify(introspector, times(1)).findIdField(entityClass);
            inOrder.verify(introspector, times(1)).hasGeneratedValue(idField);
            inOrder.verify(introspector, times(1)).findGetter(entityClass, idField);
            inOrder.verify(introspector, times(1)).findSetter(entityClass, idField);
            verifyNoMoreInteractions(introspector);
        }
    }

    @Nested
    @DisplayName("Skip metadata paths")
    class SkipMetadata {

        @Test
        @DisplayName("Should return skip metadata when entity has no @Table annotation")
        void shouldReturnSkipMetadata_whenNoTableAnnotation() {
            // Given
            Class<?> entityClass = ValidEntity.class;
            when(introspector.findTableName(entityClass)).thenReturn(Optional.empty());

            // When
            EntityMetadata metadata = resolver.resolve(entityClass);

            // Then
            assertThat(metadata.isSkip()).isTrue();
            verify(introspector, times(1)).findTableName(entityClass);
            verifyNoMoreInteractions(introspector);
        }

        @Test
        @DisplayName("Should return skip metadata when entity uses @EmbeddedId")
        void shouldReturnSkipMetadata_whenEntityUsesEmbeddedId() {
            // Given
            Class<?> entityClass = ValidEntity.class;
            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass)).thenReturn(true);

            // When
            EntityMetadata metadata = resolver.resolve(entityClass);

            // Then
            assertThat(metadata.isSkip()).isTrue();

            InOrder inOrder = inOrder(introspector);
            inOrder.verify(introspector, times(1)).findTableName(entityClass);
            inOrder.verify(introspector, times(1)).hasEmbeddedId(entityClass);
            verifyNoMoreInteractions(introspector);
        }

        @Test
        @DisplayName("Should return skip metadata when entity has no @Id field")
        void shouldReturnSkipMetadata_whenNoIdField() {
            // Given
            Class<?> entityClass = ValidEntity.class;
            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass)).thenReturn(false);
            when(introspector.findIdField(entityClass)).thenReturn(Optional.empty());

            // When
            EntityMetadata metadata = resolver.resolve(entityClass);

            // Then
            assertThat(metadata.isSkip()).isTrue();

            InOrder inOrder = inOrder(introspector);
            inOrder.verify(introspector, times(1)).findTableName(entityClass);
            inOrder.verify(introspector, times(1)).hasEmbeddedId(entityClass);
            inOrder.verify(introspector, times(1)).findIdField(entityClass);
            verifyNoMoreInteractions(introspector);
        }

        @Test
        @DisplayName("Should return skip metadata when @Id field uses @GeneratedValue")
        void shouldReturnSkipMetadata_whenIdFieldUsesGeneratedValue() {
            // Given
            Class<?> entityClass = ValidEntity.class;
            Field idField = mock(Field.class);

            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass)).thenReturn(false);
            when(introspector.findIdField(entityClass)).thenReturn(Optional.of(idField));
            when(introspector.hasGeneratedValue(idField)).thenReturn(true);

            // When
            EntityMetadata metadata = resolver.resolve(entityClass);

            // Then
            assertThat(metadata.isSkip()).isTrue();

            InOrder inOrder = inOrder(introspector);
            inOrder.verify(introspector, times(1)).findTableName(entityClass);
            inOrder.verify(introspector, times(1)).hasEmbeddedId(entityClass);
            inOrder.verify(introspector, times(1)).findIdField(entityClass);
            inOrder.verify(introspector, times(1)).hasGeneratedValue(idField);
            verifyNoMoreInteractions(introspector);
        }
    }

    @Nested
    @DisplayName("Collaborator exception propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should return skip metadata when getter throws NoSuchMethodException")
        void shouldReturnSkipMetadata_whenGetterThrowsException() throws Exception {
            // Given
            Class<?> entityClass = ValidEntity.class;
            Field idField = mock(Field.class);

            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass)).thenReturn(false);
            when(introspector.findIdField(entityClass)).thenReturn(Optional.of(idField));
            when(introspector.hasGeneratedValue(idField)).thenReturn(false);
            when(introspector.findGetter(entityClass, idField))
                    .thenThrow(new NoSuchMethodException("Getter not found"));

            // When
            EntityMetadata metadata = resolver.resolve(entityClass);

            // Then
            assertThat(metadata.isSkip()).isTrue();

            InOrder inOrder = inOrder(introspector);
            inOrder.verify(introspector, times(1)).findTableName(entityClass);
            inOrder.verify(introspector, times(1)).hasEmbeddedId(entityClass);
            inOrder.verify(introspector, times(1)).findIdField(entityClass);
            inOrder.verify(introspector, times(1)).hasGeneratedValue(idField);
            inOrder.verify(introspector, times(1)).findGetter(entityClass, idField);
            verifyNoMoreInteractions(introspector);
        }

        @Test
        @DisplayName("Should return skip metadata when setter throws NoSuchMethodException")
        void shouldReturnSkipMetadata_whenSetterThrowsException() throws Exception {
            // Given
            Class<?> entityClass = ValidEntity.class;
            Field idField = mock(Field.class);
            Method getter = mock(Method.class);

            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass)).thenReturn(false);
            when(introspector.findIdField(entityClass)).thenReturn(Optional.of(idField));
            when(introspector.hasGeneratedValue(idField)).thenReturn(false);
            when(introspector.findGetter(entityClass, idField)).thenReturn(getter);
            when(introspector.findSetter(entityClass, idField))
                    .thenThrow(new NoSuchMethodException("Setter not found"));

            // When
            EntityMetadata metadata = resolver.resolve(entityClass);

            // Then
            assertThat(metadata.isSkip()).isTrue();

            InOrder inOrder = inOrder(introspector);
            inOrder.verify(introspector, times(1)).findTableName(entityClass);
            inOrder.verify(introspector, times(1)).hasEmbeddedId(entityClass);
            inOrder.verify(introspector, times(1)).findIdField(entityClass);
            inOrder.verify(introspector, times(1)).hasGeneratedValue(idField);
            inOrder.verify(introspector, times(1)).findGetter(entityClass, idField);
            inOrder.verify(introspector, times(1)).findSetter(entityClass, idField);
            verifyNoMoreInteractions(introspector);
        }

        @Test
        @DisplayName("Should return skip metadata when introspector throws RuntimeException on findTableName")
        void shouldReturnSkipMetadata_whenIntrospectorThrowsRuntimeException() {
            // Given
            Class<?> entityClass = ValidEntity.class;
            when(introspector.findTableName(entityClass))
                    .thenThrow(new RuntimeException("Unexpected reflection error"));

            // When
            EntityMetadata metadata = resolver.resolve(entityClass);

            // Then
            assertThat(metadata.isSkip()).isTrue();

            verify(introspector, times(1)).findTableName(entityClass);
            verifyNoMoreInteractions(introspector);
        }

        @Test
        @DisplayName("Should return skip metadata when hasEmbeddedId throws RuntimeException")
        void shouldReturnSkipMetadata_whenHasEmbeddedIdThrowsRuntimeException() {
            // Given
            Class<?> entityClass = ValidEntity.class;
            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass))
                    .thenThrow(new RuntimeException("EmbeddedId check failed"));

            // When
            EntityMetadata metadata = resolver.resolve(entityClass);

            // Then
            assertThat(metadata.isSkip()).isTrue();

            InOrder inOrder = inOrder(introspector);
            inOrder.verify(introspector, times(1)).findTableName(entityClass);
            inOrder.verify(introspector, times(1)).hasEmbeddedId(entityClass);
            verifyNoMoreInteractions(introspector);
        }

        @Test
        @DisplayName("Should return skip metadata when findIdField throws RuntimeException")
        void shouldReturnSkipMetadata_whenFindIdFieldThrowsRuntimeException() {
            // Given
            Class<?> entityClass = ValidEntity.class;
            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass)).thenReturn(false);
            when(introspector.findIdField(entityClass))
                    .thenThrow(new RuntimeException("Id field lookup failed"));

            // When
            EntityMetadata metadata = resolver.resolve(entityClass);

            // Then
            assertThat(metadata.isSkip()).isTrue();

            InOrder inOrder = inOrder(introspector);
            inOrder.verify(introspector, times(1)).findTableName(entityClass);
            inOrder.verify(introspector, times(1)).hasEmbeddedId(entityClass);
            inOrder.verify(introspector, times(1)).findIdField(entityClass);
            verifyNoMoreInteractions(introspector);
        }

        @Test
        @DisplayName("Should return skip metadata when hasGeneratedValue throws RuntimeException")
        void shouldReturnSkipMetadata_whenHasGeneratedValueThrowsRuntimeException() {
            // Given
            Class<?> entityClass = ValidEntity.class;
            Field mockField = mock(Field.class);

            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass)).thenReturn(false);
            when(introspector.findIdField(entityClass)).thenReturn(Optional.of(mockField));
            when(introspector.hasGeneratedValue(mockField))
                    .thenThrow(new RuntimeException("GeneratedValue check failed"));

            // When
            EntityMetadata metadata = resolver.resolve(entityClass);

            // Then
            assertThat(metadata.isSkip()).isTrue();

            InOrder inOrder = inOrder(introspector);
            inOrder.verify(introspector, times(1)).findTableName(entityClass);
            inOrder.verify(introspector, times(1)).hasEmbeddedId(entityClass);
            inOrder.verify(introspector, times(1)).findIdField(entityClass);
            inOrder.verify(introspector, times(1)).hasGeneratedValue(mockField);
            verifyNoMoreInteractions(introspector);
        }
    }

    @Nested
    @DisplayName("Caching behavior")
    class CachingBehavior {

        @Test
        @DisplayName("Should cache metadata and not call introspector on second resolve")
        void shouldCacheMetadata_whenResolvedTwiceForSameEntity() throws NoSuchMethodException {
            // Given
            Class<?> entityClass = ValidEntity.class;
            Field mockField = mock(Field.class);
            when(mockField.getName()).thenReturn(ID_FIELD_NAME);

            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass)).thenReturn(false);
            when(introspector.findIdField(entityClass)).thenReturn(Optional.of(mockField));
            when(introspector.hasGeneratedValue(mockField)).thenReturn(false);
            when(introspector.findGetter(entityClass, mockField)).thenReturn(mock(Method.class));
            when(introspector.findSetter(entityClass, mockField)).thenReturn(mock(Method.class));

            // When
            EntityMetadata firstCall = resolver.resolve(entityClass);
            EntityMetadata secondCall = resolver.resolve(entityClass);

            // Then
            assertThat(firstCall).isSameAs(secondCall);
            verify(introspector, times(EXPECTED_SINGLE_INVOCATION)).findTableName(entityClass);
            verifyNoMoreInteractions(introspector);
        }

        @Test
        @DisplayName("Should clear cache and return zero size")
        void shouldClearCache_whenClearCacheCalled() throws NoSuchMethodException {
            // Given
            Class<?> entityClass = ValidEntity.class;
            Field mockField = mock(Field.class);
            when(mockField.getName()).thenReturn(ID_FIELD_NAME);

            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass)).thenReturn(false);
            when(introspector.findIdField(entityClass)).thenReturn(Optional.of(mockField));
            when(introspector.hasGeneratedValue(mockField)).thenReturn(false);
            when(introspector.findGetter(entityClass, mockField)).thenReturn(mock(Method.class));
            when(introspector.findSetter(entityClass, mockField)).thenReturn(mock(Method.class));

            resolver.resolve(entityClass);
            int initialCacheSize = resolver.getCacheSize();

            // When
            resolver.clearCache();

            // Then
            assertThat(resolver.getCacheSize()).isZero();
            assertThat(initialCacheSize).isEqualTo(EXPECTED_SINGLE_CACHE_SIZE);
            verify(introspector, times(1)).findTableName(entityClass);
            verifyNoMoreInteractions(introspector);
        }

        @Test
        @DisplayName("Should return correct cache size for multiple entities")
        void shouldReturnCorrectCacheSize_whenMultipleEntitiesResolved() throws NoSuchMethodException {
            // Given
            Field mockField1 = mock(Field.class);
            when(mockField1.getName()).thenReturn(ID_FIELD_NAME);
            Field mockField2 = mock(Field.class);
            when(mockField2.getName()).thenReturn(ANOTHER_ID_FIELD_NAME);

            when(introspector.findTableName(ValidEntity.class)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(ValidEntity.class)).thenReturn(false);
            when(introspector.findIdField(ValidEntity.class)).thenReturn(Optional.of(mockField1));
            when(introspector.hasGeneratedValue(mockField1)).thenReturn(false);
            when(introspector.findGetter(ValidEntity.class, mockField1)).thenReturn(mock(Method.class));
            when(introspector.findSetter(ValidEntity.class, mockField1)).thenReturn(mock(Method.class));

            when(introspector.findTableName(AnotherEntity.class)).thenReturn(Optional.of(ANOTHER_TABLE_NAME));
            when(introspector.hasEmbeddedId(AnotherEntity.class)).thenReturn(false);
            when(introspector.findIdField(AnotherEntity.class)).thenReturn(Optional.of(mockField2));
            when(introspector.hasGeneratedValue(mockField2)).thenReturn(false);
            when(introspector.findGetter(AnotherEntity.class, mockField2)).thenReturn(mock(Method.class));
            when(introspector.findSetter(AnotherEntity.class, mockField2)).thenReturn(mock(Method.class));

            // When
            resolver.resolve(ValidEntity.class);
            resolver.resolve(AnotherEntity.class);
            int cacheSize = resolver.getCacheSize();

            // Then
            assertThat(cacheSize).isEqualTo(EXPECTED_DOUBLE_CACHE_SIZE);
            verify(introspector, times(1)).findTableName(ValidEntity.class);
            verify(introspector, times(1)).findTableName(AnotherEntity.class);
            verifyNoMoreInteractions(introspector);
        }

        @Test
        @DisplayName("Should handle concurrent resolve calls for same entity safely")
        void shouldHandleConcurrentResolveCalls_whenSameEntityResolvedConcurrently() throws InterruptedException, NoSuchMethodException {
            // Given
            Class<?> entityClass = ValidEntity.class;
            Field mockField = mock(Field.class);
            when(mockField.getName()).thenReturn(ID_FIELD_NAME);

            when(introspector.findTableName(entityClass)).thenReturn(Optional.of(TABLE_NAME));
            when(introspector.hasEmbeddedId(entityClass)).thenReturn(false);
            when(introspector.findIdField(entityClass)).thenReturn(Optional.of(mockField));
            when(introspector.hasGeneratedValue(mockField)).thenReturn(false);
            when(introspector.findGetter(entityClass, mockField)).thenReturn(mock(Method.class));
            when(introspector.findSetter(entityClass, mockField)).thenReturn(mock(Method.class));

            // When
            Thread thread1 = new Thread(() -> resolver.resolve(entityClass));
            Thread thread2 = new Thread(() -> resolver.resolve(entityClass));

            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            // Then
            assertThat(resolver.getCacheSize()).isEqualTo(EXPECTED_SINGLE_CACHE_SIZE);
            verify(introspector, times(EXPECTED_SINGLE_INVOCATION)).findTableName(entityClass);
            verifyNoMoreInteractions(introspector);
        }
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
