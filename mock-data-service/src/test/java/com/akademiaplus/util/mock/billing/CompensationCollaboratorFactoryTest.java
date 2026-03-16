/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CompensationCollaboratorFactory}.
 */
@DisplayName("CompensationCollaboratorFactory")
class CompensationCollaboratorFactoryTest {

    private CompensationCollaboratorFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CompensationCollaboratorFactory();
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and all IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndAllIdsSet() {
            // Given
            factory.setAvailableCompensationIds(List.of(10L, 20L));
            factory.setAvailableCollaboratorIds(List.of(30L, 40L));

            // When
            List<CompensationCollaboratorRecord> result = factory.generate(3);

            // Then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            factory.setAvailableCompensationIds(List.of(10L));
            factory.setAvailableCollaboratorIds(List.of(30L));

            // When
            List<CompensationCollaboratorRecord> result = factory.generate(0);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Stateful constraint validation")
    class StatefulConstraintValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when compensation IDs are not set")
        void shouldThrowIllegalStateException_whenCompensationIdsNotSet() {
            // Given
            factory.setAvailableCollaboratorIds(List.of(30L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(CompensationCollaboratorFactory.ERROR_COMPENSATION_IDS_NOT_SET);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when collaborator IDs are not set")
        void shouldThrowIllegalStateException_whenCollaboratorIdsNotSet() {
            // Given
            factory.setAvailableCompensationIds(List.of(10L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(CompensationCollaboratorFactory.ERROR_COLLABORATOR_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Deduplication")
    class Deduplication {

        @Test
        @DisplayName("Should produce unique pairs only")
        void shouldProduceUniquePairsOnly() {
            // Given
            factory.setAvailableCompensationIds(List.of(1L, 2L));
            factory.setAvailableCollaboratorIds(List.of(100L, 200L));

            // When
            List<CompensationCollaboratorRecord> result = factory.generate(4);

            // Then
            long uniqueKeys = result.stream()
                    .map(r -> r.compensationId() + ":" + r.collaboratorId())
                    .distinct().count();
            assertThat(uniqueKeys).isEqualTo(result.size());
        }
    }

    @Nested
    @DisplayName("Record population")
    class RecordPopulation {

        @Test
        @DisplayName("Should populate all fields including assigned date when generating single record")
        void shouldPopulateAllFields_whenGeneratingSingleRecord() {
            // Given
            factory.setAvailableCompensationIds(List.of(10L));
            factory.setAvailableCollaboratorIds(List.of(30L));

            // When
            List<CompensationCollaboratorRecord> result = factory.generate(1);
            CompensationCollaboratorRecord record = result.get(0);

            // Then
            assertThat(record.compensationId()).isEqualTo(10L);
            assertThat(record.collaboratorId()).isEqualTo(30L);
            assertThat(record.assignedDate()).isNotNull();
            assertThat(record.assignedDate()).isBeforeOrEqualTo(LocalDate.now());
            assertThat(record.assignedDate()).isAfterOrEqualTo(LocalDate.now().minusDays(90));
        }
    }
}
