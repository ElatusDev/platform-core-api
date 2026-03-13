/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SequentialIDGenerator}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SequentialIDGenerator")
class SequentialIDGeneratorTest {

    // Test data constants
    private static final Long TENANT_ID = 1L;
    private static final String ENTITY_NAME = "course";
    private static final String EMPTY_STRING = "";
    private static final String WHITESPACE_STRING = "   ";

    // Sequence values
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

    // Version values
    private static final int VERSION_0 = 0;
    private static final int VERSION_1 = 1;
    private static final int VERSION_5 = 5;
    private static final int VERSION_10 = 10;

    // Count/Range values
    private static final int COUNT_1 = 1;
    private static final int COUNT_5 = 5;
    private static final int COUNT_10 = 10;
    private static final int COUNT_50 = 50;
    private static final int COUNT_1000 = 1000;
    private static final int COUNT_EXCEEDS_LIMIT = 10001;
    private static final int MAX_BATCH_SIZE = 10000;

    // Expected size values
    private static final int EXPECTED_SIZE_50 = 50;

    @Mock
    private TenantSequenceRepository repository;

    private SequentialIDGenerator idGenerator;

    @BeforeEach
    void setUp() {
        idGenerator = new SequentialIDGenerator(repository);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Input validation (Rules 7 + 8) — shared across all three methods
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        // ── generateId parameter validation ────────────────────────────

        @Test
        @DisplayName("Should throw IllegalArgumentException when entity name is null for generateId")
        void shouldThrowIllegalArgumentException_whenEntityNameIsNullForGenerateId() {
            // Given: null entity name

            // When/Then: should throw with correct message
            assertThatThrownBy(() -> idGenerator.generateId(null, TENANT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_ENTITY_NAME_EMPTY);

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when entity name is empty for generateId")
        void shouldThrowIllegalArgumentException_whenEntityNameIsEmptyForGenerateId() {
            // Given: empty entity name

            // When/Then: should throw with correct message
            assertThatThrownBy(() -> idGenerator.generateId(EMPTY_STRING, TENANT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_ENTITY_NAME_EMPTY);

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when entity name is whitespace for generateId")
        void shouldThrowIllegalArgumentException_whenEntityNameIsWhitespaceForGenerateId() {
            // Given: whitespace entity name

            // When/Then: should throw with correct message
            assertThatThrownBy(() -> idGenerator.generateId(WHITESPACE_STRING, TENANT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_ENTITY_NAME_EMPTY);

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant ID is null for generateId")
        void shouldThrowIllegalArgumentException_whenTenantIdIsNullForGenerateId() {
            // Given: null tenant ID

            // When/Then: should throw with correct message
            assertThatThrownBy(() -> idGenerator.generateId(ENTITY_NAME, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_TENANT_ID_NULL);

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, -1L, -100L})
        @DisplayName("Should throw IllegalArgumentException when tenant ID is non-positive for generateId")
        void shouldThrowIllegalArgumentException_whenTenantIdIsNonPositiveForGenerateId(Long invalidTenantId) {
            // Given: non-positive tenant ID

            // When/Then: should throw with correct message
            assertThatThrownBy(() -> idGenerator.generateId(ENTITY_NAME, invalidTenantId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_TENANT_ID_NOT_POSITIVE);

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        // ── generateIds parameter validation ───────────────────────────

        @Test
        @DisplayName("Should throw IllegalArgumentException when entity name is null for generateIds")
        void shouldThrowIllegalArgumentException_whenEntityNameIsNullForGenerateIds() {
            // Given: null entity name

            // When/Then: should throw with correct message
            assertThatThrownBy(() -> idGenerator.generateIds(null, TENANT_ID, COUNT_5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_ENTITY_NAME_EMPTY);

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant ID is null for generateIds")
        void shouldThrowIllegalArgumentException_whenTenantIdIsNullForGenerateIds() {
            // Given: null tenant ID

            // When/Then: should throw with correct message
            assertThatThrownBy(() -> idGenerator.generateIds(ENTITY_NAME, null, COUNT_5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_TENANT_ID_NULL);

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("Should throw IllegalArgumentException when count is non-positive for generateIds")
        void shouldThrowIllegalArgumentException_whenCountIsNonPositiveForGenerateIds(int invalidCount) {
            // Given: non-positive count

            // When/Then: should throw with correct message
            assertThatThrownBy(() -> idGenerator.generateIds(ENTITY_NAME, TENANT_ID, invalidCount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_COUNT_NOT_POSITIVE);

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when count exceeds limit for generateIds")
        void shouldThrowIllegalArgumentException_whenCountExceedsLimitForGenerateIds() {
            // Given: count exceeding MAX_BATCH_SIZE

            // When/Then: should throw with correct message referencing constant
            assertThatThrownBy(() -> idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_EXCEEDS_LIMIT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            String.format(SequentialIDGenerator.ERROR_COUNT_EXCEEDS_LIMIT, MAX_BATCH_SIZE));

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        // ── reserveRange parameter validation ──────────────────────────

        @Test
        @DisplayName("Should throw IllegalArgumentException when entity name is null for reserveRange")
        void shouldThrowIllegalArgumentException_whenEntityNameIsNullForReserveRange() {
            // Given: null entity name

            // When/Then: should throw with correct message
            assertThatThrownBy(() -> idGenerator.reserveRange(null, TENANT_ID, COUNT_10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_ENTITY_NAME_EMPTY);

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant ID is null for reserveRange")
        void shouldThrowIllegalArgumentException_whenTenantIdIsNullForReserveRange() {
            // Given: null tenant ID

            // When/Then: should throw with correct message
            assertThatThrownBy(() -> idGenerator.reserveRange(ENTITY_NAME, null, COUNT_10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_TENANT_ID_NULL);

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("Should throw IllegalArgumentException when range size is non-positive for reserveRange")
        void shouldThrowIllegalArgumentException_whenRangeSizeIsNonPositiveForReserveRange(int invalidSize) {
            // Given: non-positive range size

            // When/Then: should throw with correct message
            assertThatThrownBy(() -> idGenerator.reserveRange(ENTITY_NAME, TENANT_ID, invalidSize))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(SequentialIDGenerator.ERROR_COUNT_NOT_POSITIVE);

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when range size exceeds limit for reserveRange")
        void shouldThrowIllegalArgumentException_whenRangeSizeExceedsLimitForReserveRange() {
            // Given: range size exceeding MAX_BATCH_SIZE

            // When/Then: should throw with correct message referencing constant
            assertThatThrownBy(() -> idGenerator.reserveRange(ENTITY_NAME, TENANT_ID, COUNT_EXCEEDS_LIMIT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            String.format(SequentialIDGenerator.ERROR_COUNT_EXCEEDS_LIMIT, MAX_BATCH_SIZE));

            // Rule 9: downstream mock not called
            verifyNoInteractions(repository);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // generateId method
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateId")
    class GenerateIdTests {

        @Test
        @DisplayName("Should generate ID for existing sequence")
        void shouldGenerateIdForExistingSequence_whenSequenceExists() {
            // Given: existing sequence with value 5
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence existingSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_5)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(existingSequence));
            when(repository.save(existingSequence)).thenReturn(existingSequence);

            // When: generating ID
            Long generatedId = idGenerator.generateId(ENTITY_NAME, TENANT_ID);

            // Then: should return current next value and increment
            assertThat(generatedId).isEqualTo(SEQUENCE_VALUE_5);

            ArgumentCaptor<TenantSequence> sequenceCaptor = ArgumentCaptor.forClass(TenantSequence.class);
            InOrder order = inOrder(repository);
            order.verify(repository, times(1)).findById(sequenceId);
            order.verify(repository, times(1)).save(sequenceCaptor.capture());
            assertThat(sequenceCaptor.getValue().getNextValue()).isEqualTo(SEQUENCE_VALUE_6);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should initialize new sequence when sequence does not exist")
        void shouldInitializeNewSequence_whenSequenceDoesNotExist() {
            // Given: no existing sequence
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence newSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(INITIAL_SEQUENCE_VALUE)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.empty());
            when(repository.saveAndFlush(newSequence)).thenReturn(newSequence);
            when(repository.save(newSequence)).thenReturn(newSequence);

            // When: generating ID
            Long generatedId = idGenerator.generateId(ENTITY_NAME, TENANT_ID);

            // Then: should return initial value
            assertThat(generatedId).isEqualTo(INITIAL_SEQUENCE_VALUE);

            ArgumentCaptor<TenantSequence> flushCaptor = ArgumentCaptor.forClass(TenantSequence.class);
            verify(repository, times(1)).findById(sequenceId);
            verify(repository, times(1)).saveAndFlush(flushCaptor.capture());
            verify(repository, times(1)).save(newSequence);
            assertThat(flushCaptor.getValue().getNextValue()).isEqualTo(INITIAL_SEQUENCE_VALUE);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should handle concurrent sequence creation via DataIntegrityViolationException")
        void shouldHandleConcurrentSequenceCreation_whenDataIntegrityViolationOccurs() {
            // Given: concurrent creation scenario
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence existingSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(INITIAL_SEQUENCE_VALUE)
                    .version(VERSION_0)
                    .build();

            TenantSequence newSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(INITIAL_SEQUENCE_VALUE)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(existingSequence));

            when(repository.saveAndFlush(newSequence))
                    .thenThrow(new DataIntegrityViolationException("Duplicate key"));

            when(repository.save(existingSequence)).thenReturn(existingSequence);

            // When: generating ID
            Long generatedId = idGenerator.generateId(ENTITY_NAME, TENANT_ID);

            // Then: should recover and return ID
            assertThat(generatedId).isEqualTo(INITIAL_SEQUENCE_VALUE);
            verify(repository, times(1)).saveAndFlush(newSequence);
            verify(repository, times(2)).findById(sequenceId);
            verify(repository, times(1)).save(existingSequence);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should throw IDGenerationException when concurrent creation fails and sequence not found")
        void shouldThrowIDGenerationException_whenConcurrentCreationFailsAndSequenceNotFound() {
            // Given: concurrent creation where second findById also empty
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence newSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(INITIAL_SEQUENCE_VALUE)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.empty());

            when(repository.saveAndFlush(newSequence))
                    .thenThrow(new DataIntegrityViolationException("Duplicate key"));

            // When/Then: should throw IDGenerationException with correct message
            assertThatThrownBy(() -> idGenerator.generateId(ENTITY_NAME, TENANT_ID))
                    .isInstanceOf(IDGenerationException.class)
                    .hasMessage(String.format(
                            SequentialIDGenerator.ERROR_FAILED_TO_INITIALIZE_SEQUENCE, ENTITY_NAME, TENANT_ID));

            // Rule 9: save() never called
            verify(repository, times(2)).findById(sequenceId);
            verify(repository, times(1)).saveAndFlush(newSequence);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should propagate OptimisticLockException when save fails with version conflict")
        void shouldPropagateOptimisticLockException_whenSaveFailsWithVersionConflict() {
            // Given: optimistic lock conflict on save
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_10)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(sequence))
                    .thenThrow(new OptimisticLockException("Version mismatch"));

            // When/Then: OptimisticLockException should propagate
            assertThatThrownBy(() -> idGenerator.generateId(ENTITY_NAME, TENANT_ID))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessage("Version mismatch");

            verify(repository, times(1)).findById(sequenceId);
            verify(repository, times(1)).save(sequence);
            verifyNoMoreInteractions(repository);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // generateIds method
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("generateIds")
    class GenerateIdsTests {

        @Test
        @DisplayName("Should generate multiple IDs for existing sequence")
        void shouldGenerateMultipleIds_whenSequenceExists() {
            // Given: existing sequence with value 10
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_10)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(sequence)).thenReturn(sequence);

            Long[] expectedIds = {10L, 11L, 12L, 13L, 14L};

            // When: generating 5 IDs
            List<Long> ids = idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_5);

            // Then: should return sequential IDs
            assertThat(ids).containsExactly(expectedIds);

            ArgumentCaptor<TenantSequence> captor = ArgumentCaptor.forClass(TenantSequence.class);
            InOrder order = inOrder(repository);
            order.verify(repository, times(1)).findById(sequenceId);
            order.verify(repository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getNextValue()).isEqualTo(SEQUENCE_VALUE_15);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should generate large batch of IDs")
        void shouldGenerateLargeBatchOfIds_whenCountIsLarge() {
            // Given: existing sequence with value 100
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_100)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(sequence)).thenReturn(sequence);

            Long expectedLastId = 149L;

            // When: generating 50 IDs
            List<Long> ids = idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_50);

            // Then: should return 50 sequential IDs
            assertThat(ids).hasSize(EXPECTED_SIZE_50);
            assertThat(ids).first().isEqualTo(SEQUENCE_VALUE_100);
            assertThat(ids).last().isEqualTo(expectedLastId);

            verify(repository, times(1)).findById(sequenceId);
            verify(repository, times(1)).save(sequence);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should initialize sequence when generating multiple IDs for new entity")
        void shouldInitializeSequence_whenGeneratingMultipleIdsForNewEntity() {
            // Given: no existing sequence
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence newSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(INITIAL_SEQUENCE_VALUE)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.empty());
            when(repository.saveAndFlush(newSequence)).thenReturn(newSequence);
            when(repository.save(newSequence)).thenReturn(newSequence);

            Long[] expectedIds = {1L, 2L, 3L, 4L, 5L};

            // When: generating 5 IDs
            List<Long> ids = idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_5);

            // Then: should return IDs starting from 1
            assertThat(ids).containsExactly(expectedIds);

            ArgumentCaptor<TenantSequence> flushCaptor = ArgumentCaptor.forClass(TenantSequence.class);
            verify(repository, times(1)).findById(sequenceId);
            verify(repository, times(1)).saveAndFlush(flushCaptor.capture());
            verify(repository, times(1)).save(newSequence);
            assertThat(flushCaptor.getValue().getNextValue()).isEqualTo(INITIAL_SEQUENCE_VALUE);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should throw IDGenerationException when batch allocation would overflow")
        void shouldThrowIDGenerationException_whenBatchAllocationWouldOverflow() {
            // Given: sequence near Long.MAX_VALUE
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(MAX_VALUE_MINUS_100)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));

            // When/Then: should throw with overflow message from constant
            assertThatThrownBy(() -> idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_1000))
                    .isInstanceOf(IDGenerationException.class)
                    .hasMessage(String.format(
                            SequentialIDGenerator.ERROR_ID_RANGE_OVERFLOW_ALLOCATE,
                            ENTITY_NAME, TENANT_ID, COUNT_1000, MAX_VALUE_MINUS_100));

            // Rule 9: save never called after overflow detection
            verify(repository, times(1)).findById(sequenceId);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should propagate OptimisticLockException when save fails during batch generation")
        void shouldPropagateOptimisticLockException_whenSaveFailsDuringBatchGeneration() {
            // Given: optimistic lock conflict on save
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_10)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(sequence))
                    .thenThrow(new OptimisticLockException("Version mismatch"));

            // When/Then: OptimisticLockException should propagate
            assertThatThrownBy(() -> idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_5))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessage("Version mismatch");

            verify(repository, times(1)).findById(sequenceId);
            verify(repository, times(1)).save(sequence);
            verifyNoMoreInteractions(repository);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // reserveRange method
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("reserveRange")
    class ReserveRangeTests {

        @Test
        @DisplayName("Should reserve range for existing sequence")
        void shouldReserveRange_whenSequenceExists() {
            // Given: existing sequence with value 100
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_100)
                    .version(VERSION_5)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(sequence)).thenReturn(sequence);

            // When: reserving 50 IDs
            Long startId = idGenerator.reserveRange(ENTITY_NAME, TENANT_ID, COUNT_50);

            // Then: should return start of reserved range
            assertThat(startId).isEqualTo(SEQUENCE_VALUE_100);

            ArgumentCaptor<TenantSequence> captor = ArgumentCaptor.forClass(TenantSequence.class);
            InOrder order = inOrder(repository);
            order.verify(repository, times(1)).findById(sequenceId);
            order.verify(repository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getNextValue()).isEqualTo(SEQUENCE_VALUE_150);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should initialize sequence when reserving range for new entity")
        void shouldInitializeSequence_whenReservingRangeForNewEntity() {
            // Given: no existing sequence
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence newSequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(INITIAL_SEQUENCE_VALUE)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.empty());
            when(repository.saveAndFlush(newSequence)).thenReturn(newSequence);
            when(repository.save(newSequence)).thenReturn(newSequence);

            // When: reserving 10 IDs
            Long startId = idGenerator.reserveRange(ENTITY_NAME, TENANT_ID, COUNT_10);

            // Then: should return initial value
            assertThat(startId).isEqualTo(INITIAL_SEQUENCE_VALUE);

            ArgumentCaptor<TenantSequence> flushCaptor = ArgumentCaptor.forClass(TenantSequence.class);
            verify(repository, times(1)).findById(sequenceId);
            verify(repository, times(1)).saveAndFlush(flushCaptor.capture());
            verify(repository, times(1)).save(newSequence);
            assertThat(flushCaptor.getValue().getNextValue()).isEqualTo(INITIAL_SEQUENCE_VALUE);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should throw IDGenerationException when range reservation would overflow")
        void shouldThrowIDGenerationException_whenRangeReservationWouldOverflow() {
            // Given: sequence near Long.MAX_VALUE
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(MAX_VALUE_MINUS_100)
                    .version(VERSION_0)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));

            // When/Then: should throw with overflow message from constant
            assertThatThrownBy(() -> idGenerator.reserveRange(ENTITY_NAME, TENANT_ID, COUNT_1000))
                    .isInstanceOf(IDGenerationException.class)
                    .hasMessage(String.format(
                            SequentialIDGenerator.ERROR_ID_RANGE_OVERFLOW_RESERVE,
                            ENTITY_NAME, TENANT_ID, COUNT_1000, MAX_VALUE_MINUS_100));

            // Rule 9: save never called after overflow detection
            verify(repository, times(1)).findById(sequenceId);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should propagate OptimisticLockException when save fails during range reservation")
        void shouldPropagateOptimisticLockException_whenSaveFailsDuringRangeReservation() {
            // Given: optimistic lock conflict on save
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_100)
                    .version(VERSION_5)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(sequence))
                    .thenThrow(new OptimisticLockException("Version mismatch"));

            // When/Then: OptimisticLockException should propagate
            assertThatThrownBy(() -> idGenerator.reserveRange(ENTITY_NAME, TENANT_ID, COUNT_10))
                    .isInstanceOf(OptimisticLockException.class)
                    .hasMessage("Version mismatch");

            verify(repository, times(1)).findById(sequenceId);
            verify(repository, times(1)).save(sequence);
            verifyNoMoreInteractions(repository);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Edge cases
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle maximum Long value minus one for generateId")
        void shouldHandleMaxLongValueMinusOne_whenGeneratingId() {
            // Given: sequence at Long.MAX_VALUE - 1
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(MAX_VALUE_MINUS_ONE)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(sequence)).thenReturn(sequence);

            // When: generating ID
            Long generatedId = idGenerator.generateId(ENTITY_NAME, TENANT_ID);

            // Then: should return MAX_VALUE - 1
            assertThat(generatedId).isEqualTo(MAX_VALUE_MINUS_ONE);

            ArgumentCaptor<TenantSequence> captor = ArgumentCaptor.forClass(TenantSequence.class);
            verify(repository, times(1)).findById(sequenceId);
            verify(repository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getNextValue()).isEqualTo(Long.MAX_VALUE);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should generate single ID in batch request")
        void shouldGenerateSingleIdInBatch_whenCountIsOne() {
            // Given: existing sequence with value 42
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_42)
                    .version(VERSION_1)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(sequence)).thenReturn(sequence);

            // When: generating 1 ID in batch
            List<Long> ids = idGenerator.generateIds(ENTITY_NAME, TENANT_ID, COUNT_1);

            // Then: should return single ID
            assertThat(ids)
                    .hasSize(COUNT_1)
                    .first().isEqualTo(SEQUENCE_VALUE_42);

            verify(repository, times(1)).findById(sequenceId);
            verify(repository, times(1)).save(sequence);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should reserve minimum range of 1")
        void shouldReserveMinimumRange_whenRangeSizeIsOne() {
            // Given: existing sequence with value 1000
            TenantSequence.TenantSequenceId sequenceId =
                    new TenantSequence.TenantSequenceId(TENANT_ID, ENTITY_NAME);

            TenantSequence sequence = TenantSequence.builder()
                    .id(sequenceId)
                    .nextValue(SEQUENCE_VALUE_1000)
                    .version(VERSION_10)
                    .build();

            when(repository.findById(sequenceId)).thenReturn(Optional.of(sequence));
            when(repository.save(sequence)).thenReturn(sequence);

            // When: reserving range of 1
            Long startId = idGenerator.reserveRange(ENTITY_NAME, TENANT_ID, COUNT_1);

            // Then: should return start ID
            assertThat(startId).isEqualTo(SEQUENCE_VALUE_1000);

            ArgumentCaptor<TenantSequence> captor = ArgumentCaptor.forClass(TenantSequence.class);
            verify(repository, times(1)).findById(sequenceId);
            verify(repository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getNextValue()).isEqualTo(SEQUENCE_VALUE_1001);
            verifyNoMoreInteractions(repository);
        }
    }
}
