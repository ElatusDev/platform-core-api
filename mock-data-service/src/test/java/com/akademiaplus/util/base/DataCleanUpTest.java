package com.akademiaplus.util.base;

import com.akademiaplus.exceptions.UnprocessableDataModelException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("DataCleanUp")
@ExtendWith(MockitoExtension.class)
class DataCleanUpTest {

    @Mock private EntityManager entityManager;
    @Mock private TenantScopedRepository<AnnotatedEntity, Long> repository;
    @Mock private TenantScopedRepository<UnannotatedEntity, Long> unannotatedRepository;
    @Mock private TenantScopedRepository<InvalidTableNameEntity, Long> invalidTableNameRepository;
    @Mock private Query nativeQuery;

    private DataCleanUp<AnnotatedEntity, Long> dataCleanUp;

    @BeforeEach
    void setUp() throws Exception {
        dataCleanUp = new DataCleanUp<>(entityManager);
        dataCleanUp.setRepository(repository);
        dataCleanUp.setDataModel(AnnotatedEntity.class);
    }

    @Nested
    @DisplayName("Successful cleanup")
    class SuccessfulCleanup {

        @Test
        @DisplayName("Should delete all and reset auto increment when clean is called")
        void shouldDeleteAllAndResetAutoIncrement_whenCleanIsCalled() {
            // Given
            String expectedSql = "ALTER TABLE test_table AUTO_INCREMENT = 1";
            when(entityManager.createNativeQuery(expectedSql)).thenReturn(nativeQuery);

            // When
            dataCleanUp.clean();

            // Then — state assertion: verify SQL string contains correct table name
            assertThat(expectedSql).contains("test_table");

            // Then — interaction assertions in order: delete → reset auto-increment
            InOrder inOrder = inOrder(repository, entityManager, nativeQuery);
            inOrder.verify(repository, times(1)).deleteAllInBatch();
            inOrder.verify(entityManager, times(1)).createNativeQuery(expectedSql);
            inOrder.verify(nativeQuery, times(1)).executeUpdate();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should use table name from annotation when clean is called")
        void shouldUseTableNameFromAnnotation_whenCleanIsCalled() {
            // Given
            String expectedSql = "ALTER TABLE test_table AUTO_INCREMENT = 1";
            when(entityManager.createNativeQuery(expectedSql)).thenReturn(nativeQuery);

            // When
            dataCleanUp.clean();

            // Then — state assertion
            assertThat(expectedSql).startsWith("ALTER TABLE test_table");

            // Then — interaction assertions
            InOrder inOrder = inOrder(repository, entityManager, nativeQuery);
            inOrder.verify(repository, times(1)).deleteAllInBatch();
            inOrder.verify(entityManager, times(1)).createNativeQuery(expectedSql);
            inOrder.verify(nativeQuery, times(1)).executeUpdate();
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw UnprocessableDataModelException when table annotation missing")
        void shouldThrowUnprocessableDataModelException_whenTableAnnotationMissing() {
            // Given
            DataCleanUp<UnannotatedEntity, Long> cleanUp = new DataCleanUp<>(entityManager);
            cleanUp.setRepository(unannotatedRepository);
            cleanUp.setDataModel(UnannotatedEntity.class);

            // When / Then
            assertThatThrownBy(cleanUp::clean)
                    .isInstanceOf(UnprocessableDataModelException.class)
                    .hasMessage(DataCleanUp.ANNOTATION_MISSING);

            // Then — interaction assertion: deleteAllInBatch was called before the throw
            verify(unannotatedRepository, times(1)).deleteAllInBatch();

            // Then — downstream mocks not reached (exception thrown before createNativeQuery)
            verifyNoInteractions(nativeQuery);
            verifyNoMoreInteractions(entityManager, repository, unannotatedRepository, nativeQuery);
        }

        @Test
        @DisplayName("Should throw UnprocessableDataModelException when table name is invalid")
        void shouldThrowUnprocessableDataModelException_whenTableNameIsInvalid() {
            // Given
            DataCleanUp<InvalidTableNameEntity, Long> cleanUp = new DataCleanUp<>(entityManager);
            cleanUp.setRepository(invalidTableNameRepository);
            cleanUp.setDataModel(InvalidTableNameEntity.class);

            // When / Then
            assertThatThrownBy(cleanUp::clean)
                    .isInstanceOf(UnprocessableDataModelException.class)
                    .hasMessage(DataCleanUp.INVALID_TABLE_NAME_PREFIX + "drop--table");

            // Then — interaction assertion: deleteAllInBatch was called before the throw
            verify(invalidTableNameRepository, times(1)).deleteAllInBatch();

            // Then — downstream mocks not reached
            verifyNoInteractions(nativeQuery);
            verifyNoMoreInteractions(entityManager, invalidTableNameRepository);
        }
    }

    @Table(name = "test_table")
    static class AnnotatedEntity {}

    static class UnannotatedEntity {}

    @Table(name = "drop--table")
    static class InvalidTableNameEntity {}
}
