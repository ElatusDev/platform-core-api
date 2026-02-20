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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DataCleanUp")
@ExtendWith(MockitoExtension.class)
class DataCleanUpTest {

    @Mock private EntityManager entityManager;
    @Mock private TenantScopedRepository<AnnotatedEntity, Long> repository;
    @Mock private TenantScopedRepository<UnannotatedEntity, Long> unannotatedRepository;
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
        void shouldDeleteAllAndResetAutoIncrement_whenCleanIsCalled() {
            // Given
            String expectedSql = "ALTER TABLE test_table AUTO_INCREMENT = 1";
            when(entityManager.createNativeQuery(expectedSql)).thenReturn(nativeQuery);

            // When
            dataCleanUp.clean();

            // Then
            verify(repository).deleteAllInBatch();
            verify(entityManager).createNativeQuery(expectedSql);
            verify(nativeQuery).executeUpdate();
        }

        @Test
        void shouldUseTableNameFromAnnotation_whenCleanIsCalled() {
            // Given
            String expectedSql = "ALTER TABLE test_table AUTO_INCREMENT = 1";
            when(entityManager.createNativeQuery(expectedSql)).thenReturn(nativeQuery);

            // When
            dataCleanUp.clean();

            // Then
            verify(entityManager).createNativeQuery(expectedSql);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        void shouldThrowUnprocessableDataModelException_whenTableAnnotationMissing() {
            // Given
            DataCleanUp<UnannotatedEntity, Long> cleanUp = new DataCleanUp<>(entityManager);
            cleanUp.setRepository(unannotatedRepository);
            cleanUp.setDataModel(UnannotatedEntity.class);

            // When / Then
            assertThatThrownBy(cleanUp::clean)
                    .isInstanceOf(UnprocessableDataModelException.class);
        }
    }

    @Table(name = "test_table")
    static class AnnotatedEntity {}

    static class UnannotatedEntity {}
}
