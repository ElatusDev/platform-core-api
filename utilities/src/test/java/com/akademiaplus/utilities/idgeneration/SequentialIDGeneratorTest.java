package com.akademiaplus.utilities.idgeneration;

import com.akademiaplus.utilities.exceptions.idgeneration.IDGenerationException;
import com.akademiaplus.utilities.idgeneration.interfaceadapters.TenantSequence;
import com.akademiaplus.utilities.idgeneration.interfaceadapters.TenantSequenceRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SequentialIDGenerator
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SequentialIDGenerator Tests")
class SequentialIDGeneratorTest {

    // Test data constants - shared across multiple tests
    private static final Integer TENANT_ID = 1;
    private static final String ENTITY_NAME = "course";
    private static final String EMPTY_STRING = "";
    private static final String WHITESPACE_STRING = "   ";

    // Sequence values - shared across multiple tests
    private static final Long INITIAL_SEQUENCE_VALUE = 1L;
    private static final Long SEQUENCE_VALUE_5 = 5L;
    private static final Long SEQUENCE_VALUE_6 = 6L;
    private static final Long SEQUENCE_VALUE_10 = 10L;
    private static final Long SEQUENCE_VALUE_15 = 15L;
    private static final Long SEQUENCE_VALUE_42 = 42L;
    private static final Long SEQUENCE_VALUE_100 = 100L;
    private static final Long SEQUENCE_VALUE_150 = 150L;
    private static final Long SEQUENCE_VALUE_1000 = 1000L;
    private static final Long SEQUENCE_VALUE_1001 = 1001L;
    private static final Long MAX_VALUE_MINUS_ONE = Long.MAX_VALUE - 1;
    private static final Long MAX_VALUE_MINUS_100 = Long.MAX_VALUE - 100;

    // Version values - shared across multiple tests
    private static final int VERSION_0 = 0;
    private static final int VERSION_1 = 1;
    private static final int VERSION_5 = 5;
    private static final int VERSION_10 = 10;

    // Count/Range values - shared across multiple tests
    private static final int COUNT_1 = 1;
    private static final int COUNT_5 = 5;
    private static final int COUNT_10 = 10;
    private static final int COUNT_50 = 50;
    private static final int COUNT_1000 = 1000;
    private static final int COUNT_EXCEEDS_LIMIT = 10001;

    // Expected size values - shared across multiple tests
    private static final int EXPECTED_SIZE_50 = 50;

    // Mock call counts - shared across multiple tests
    private static final int TIMES_ONE = 1;
    private static final int TIMES_TWO = 2;

    // Error messages - shared across multiple tests
    private static final String ERROR_DUPLICATE_KEY = "Duplicate key";
    private static final String ERROR_VERSION_MISMATCH = "Version mismatch";
    private static final String ERROR_ID_RANGE_OVERFLOW = "ID range overflow";

    @Mock
    private TenantSequenceRepository repository;

    private SequentialIDGenerator idGenerator;

    @BeforeEach
    void setUp() {
        idGenerator = new SequentialIDGenerator(repository);
    }

    @Nested
    @DisplayName("generateId method")
    class GenerateIdTests {

        @Test
        @DisplayName("Should generate ID for existing sequence")
        void shouldGenerateIdForExistingSequence() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence existingSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_5)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(existingSequence));
            when(repository.save(any(TenantSequence.class))).thenReturn(existingSequence);

            // When
            Long generatedId = idGenerator.generateId(ENTITY_NAME, TENANT_ID);

            // Then
            assertThat(generatedId).isEqualTo(SEQUENCE_VALUE_5);

            ArgumentCaptor<TenantSequence> sequenceCaptor = ArgumentCaptor.forClass(TenantSequence.class);
            verify(repository).save(sequenceCaptor.capture());
            assertThat(sequenceCaptor.getValue().getNextValue()).isEqualTo(SEQUENCE_VALUE_6);
        }

        @Test
        @DisplayName("Should initialize new sequence when not exists")
        void shouldInitializeNewSequenceWhenNotExists() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence newSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(INITIAL_SEQUENCE_VALUE)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.empty());
            when(repository.saveAndFlush(any(TenantSequence.class))).thenReturn(newSequence);
            when(repository.save(any(TenantSequence.class))).thenReturn(newSequence);

            // When
            Long generatedId = idGenerator.generateId(ENTITY_NAME, TENANT_ID);

            // Then
            assertThat(generatedId).isEqualTo(INITIAL_SEQUENCE_VALUE);
            verify(repository).saveAndFlush(any(TenantSequence.class));
            verify(repository).save(any(TenantSequence.class));
        }

        @Test
        @DisplayName("Should handle concurrent sequence creation")
        void shouldHandleConcurrentSequenceCreation() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence existingSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(INITIAL_SEQUENCE_VALUE)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId))
                    .thenReturn(Optional.empty())  // First check: not exists
                    .thenReturn(Optional.of(existingSequence));  // After concurrent creation

            when(repository.saveAndFlush(any(TenantSequence.class)))
                    .thenThrow(new DataIntegrityViolationException(ERROR_DUPLICATE_KEY));

            when(repository.save(any(TenantSequence.class))).thenReturn(existingSequence);

            // When
            Long generatedId = idGenerator.generateId(ENTITY_NAME, TENANT_ID);

            // Then
            assertThat(generatedId).isEqualTo(INITIAL_SEQUENCE_VALUE);
            verify(repository).saveAndFlush(any(TenantSequence.class));
            verify(repository, times(TIMES_TWO)).findById(sequenceId);
        }

        @Test
        @DisplayName("Should throw exception when concurrent creation fails and sequence not found")
        void shouldThrowExceptionWhenConcurrentCreationFailsAndNotFound() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            when(repository.findById(sequenceId))
                    .thenReturn(Optional.empty())  // First check
                    .thenReturn(Optional.empty());  // After failed creation

            when(repository.saveAndFlush(any(TenantSequence.class)))
                    .thenThrow(new DataIntegrityViolationException(ERROR_DUPLICATE_KEY));

            int substringLength = 20;

            // When/Then
            assertThatThrownBy(() -> idGenerator.generateId(ENTITY_NAME, TENANT_ID))
                    .isInstanceOf(IDGenerationException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_FAILED_TO_INITIALIZE_SEQUENCE.substring(0, substringLength));
        }

        @Test
        @DisplayName("Should throw OptimisticLockException for retry handling")
        void shouldThrowOptimisticLockException() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_10)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(any(TenantSequence.class)))
                    .thenThrow(new OptimisticLockException(ERROR_VERSION_MISMATCH));

            // When/Then
            assertThatThrownBy(() -> idGenerator.generateId(ENTITY_NAME, TENANT_ID))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessage(ERROR_VERSION_MISMATCH);

            verify(repository, times(TIMES_ONE)).save(any(TenantSequence.class));
        }
    }

    @Nested
    @DisplayName("generateIds method")
    class GenerateIdsTests {

        @Test
        @DisplayName("Should generate multiple IDs for existing sequence")
        void shouldGenerateMultipleIds() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_10)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(any(TenantSequence.class))).thenReturn(sequence);

            Long[] expectedIds = {10L, 11L, 12L, 13L, 14L};

            // When
            List<Long> ids = idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_5);

            // Then
            assertThat(ids).containsExactly(expectedIds);

            ArgumentCaptor<TenantSequence> captor = ArgumentCaptor.forClass(TenantSequence.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getNextValue()).isEqualTo(SEQUENCE_VALUE_15);
        }

        @Test
        @DisplayName("Should generate large batch of IDs")
        void shouldGenerateLargeBatchOfIds() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_100)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(any(TenantSequence.class))).thenReturn(sequence);

            Long expectedLastId = 149L;

            // When
            List<Long> ids = idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_50);

            // Then
            assertThat(ids).hasSize(EXPECTED_SIZE_50);
            assertThat(ids).first().isEqualTo(SEQUENCE_VALUE_100);
            assertThat(ids).last().isEqualTo(expectedLastId);
        }

        @Test
        @DisplayName("Should initialize sequence when generating multiple IDs")
        void shouldInitializeSequenceWhenGeneratingMultipleIds() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence newSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(INITIAL_SEQUENCE_VALUE)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.empty());
            when(repository.saveAndFlush(any(TenantSequence.class))).thenReturn(newSequence);
            when(repository.save(any(TenantSequence.class))).thenReturn(newSequence);

            Long[] expectedIds = {1L, 2L, 3L, 4L, 5L};

            // When
            List<Long> ids = idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_5);

            // Then
            assertThat(ids).containsExactly(expectedIds);
            verify(repository).saveAndFlush(any(TenantSequence.class));
        }

        @Test
        @DisplayName("Should throw exception for ID overflow in batch")
        void shouldThrowExceptionForOverflowInBatch() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(MAX_VALUE_MINUS_100)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));

            // When/Then
            assertThatThrownBy(() -> idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_1000))
                    .isInstanceOf(IDGenerationException.class)
                    .hasMessageContaining(ERROR_ID_RANGE_OVERFLOW);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("Should throw exception for invalid count")
        void shouldThrowExceptionForInvalidCount(int invalidCount) {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_10)
                    .version(VERSION_1)
                    .build();

            // When/Then
            assertThatThrownBy(() -> idGenerator.generateIds(ENTITY_NAME, TENANT_ID, invalidCount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_COUNT_NOT_POSITIVE);
        }

        @Test
        @DisplayName("Should throw exception for count exceeding limit")
        void shouldThrowExceptionForCountExceedingLimit() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_10)
                    .version(VERSION_1)
                    .build();

            String expectedMessageFragment = "safety";

            // When/Then
            assertThatThrownBy(() -> idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_EXCEEDS_LIMIT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(expectedMessageFragment);
        }
    }

    @Nested
    @DisplayName("reserveRange method")
    class ReserveRangeTests {

        @Test
        @DisplayName("Should reserve range for existing sequence")
        void shouldReserveRangeForExistingSequence() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_100)
                    .version(VERSION_5)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(any(TenantSequence.class))).thenReturn(sequence);

            // When
            Long startId = idGenerator.reserveRange(ENTITY_NAME, TENANT_ID, COUNT_50);

            // Then
            assertThat(startId).isEqualTo(SEQUENCE_VALUE_100);

            ArgumentCaptor<TenantSequence> captor = ArgumentCaptor.forClass(TenantSequence.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getNextValue()).isEqualTo(SEQUENCE_VALUE_150);
        }

        @Test
        @DisplayName("Should initialize sequence when reserving range")
        void shouldInitializeSequenceWhenReservingRange() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence newSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(INITIAL_SEQUENCE_VALUE)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.empty());
            when(repository.saveAndFlush(any(TenantSequence.class))).thenReturn(newSequence);
            when(repository.save(any(TenantSequence.class))).thenReturn(newSequence);

            // When
            Long startId = idGenerator.reserveRange(ENTITY_NAME, TENANT_ID, COUNT_10);

            // Then
            assertThat(startId).isEqualTo(INITIAL_SEQUENCE_VALUE);
            verify(repository).saveAndFlush(any(TenantSequence.class));
        }

        @Test
        @DisplayName("Should throw exception for range overflow")
        void shouldThrowExceptionForRangeOverflow() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(MAX_VALUE_MINUS_100)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));

            // When/Then
            assertThatThrownBy(() -> idGenerator.reserveRange(ENTITY_NAME, TENANT_ID, COUNT_1000))
                    .isInstanceOf(IDGenerationException.class)
                    .hasMessageContaining(ERROR_ID_RANGE_OVERFLOW);
        }
    }

    @Nested
    @DisplayName("Parameter validation")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception for null entity name")
        void shouldThrowExceptionForNullEntityName() {
            // When/Then
            assertThatThrownBy(() -> idGenerator.generateId(null, TENANT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_ENTITY_NAME_EMPTY);
        }

        @Test
        @DisplayName("Should throw exception for empty entity name")
        void shouldThrowExceptionForEmptyEntityName() {
            // When/Then
            assertThatThrownBy(() -> idGenerator.generateId(EMPTY_STRING, TENANT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_ENTITY_NAME_EMPTY);
        }

        @Test
        @DisplayName("Should throw exception for whitespace entity name")
        void shouldThrowExceptionForWhitespaceEntityName() {
            // When/Then
            assertThatThrownBy(() -> idGenerator.generateId(WHITESPACE_STRING, TENANT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_ENTITY_NAME_EMPTY);
        }

        @Test
        @DisplayName("Should throw exception for null tenant ID")
        void shouldThrowExceptionForNullTenantId() {
            // When/Then
            assertThatThrownBy(() -> idGenerator.generateId(ENTITY_NAME, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_TENANT_ID_NULL);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("Should throw exception for non-positive tenant ID")
        void shouldThrowExceptionForNonPositiveTenantId(int invalidTenantId) {
            // When/Then
            assertThatThrownBy(() -> idGenerator.generateId(ENTITY_NAME, invalidTenantId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_TENANT_ID_NOT_POSITIVE);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle maximum Long value minus one")
        void shouldHandleMaxLongValueMinusOne() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(MAX_VALUE_MINUS_ONE)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(any(TenantSequence.class))).thenReturn(sequence);

            // When
            Long generatedId = idGenerator.generateId(ENTITY_NAME, TENANT_ID);

            // Then
            assertThat(generatedId).isEqualTo(MAX_VALUE_MINUS_ONE);

            ArgumentCaptor<TenantSequence> captor = ArgumentCaptor.forClass(TenantSequence.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getNextValue()).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("Should generate single ID in batch request")
        void shouldGenerateSingleIdInBatch() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_42)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(any(TenantSequence.class))).thenReturn(sequence);

            // When
            List<Long> ids = idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_1);

            // Then
            assertThat(ids)
                    .hasSize(COUNT_1)
                    .first().isEqualTo(SEQUENCE_VALUE_42);
        }

        @Test
        @DisplayName("Should reserve minimum range of 1")
        void shouldReserveMinimumRangeOf1() {
            // Given
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_1000)
                    .version(VERSION_10)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(any(TenantSequence.class))).thenReturn(sequence);

            // When
            Long startId = idGenerator.reserveRange(ENTITY_NAME, TENANT_ID, COUNT_1);

            // Then
            assertThat(startId).isEqualTo(SEQUENCE_VALUE_1000);

            ArgumentCaptor<TenantSequence> captor = ArgumentCaptor.forClass(TenantSequence.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getNextValue()).isEqualTo(SEQUENCE_VALUE_1001);
        }
    }
}