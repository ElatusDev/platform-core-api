/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MembershipCourseFactory}.
 */
@DisplayName("MembershipCourseFactory")
class MembershipCourseFactoryTest {

    private MembershipCourseFactory factory;

    @BeforeEach
    void setUp() {
        factory = new MembershipCourseFactory();
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and all IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndAllIdsSet() {
            // Given
            factory.setAvailableMembershipIds(List.of(10L, 20L));
            factory.setAvailableCourseIds(List.of(30L, 40L));

            // When
            List<MembershipCourseRecord> result = factory.generate(3);

            // Then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            factory.setAvailableMembershipIds(List.of(10L));
            factory.setAvailableCourseIds(List.of(30L));

            // When
            List<MembershipCourseRecord> result = factory.generate(0);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Stateful constraint validation")
    class StatefulConstraintValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when membership IDs are not set")
        void shouldThrowIllegalStateException_whenMembershipIdsNotSet() {
            // Given
            factory.setAvailableCourseIds(List.of(30L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(MembershipCourseFactory.ERROR_MEMBERSHIP_IDS_NOT_SET);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when course IDs are not set")
        void shouldThrowIllegalStateException_whenCourseIdsNotSet() {
            // Given
            factory.setAvailableMembershipIds(List.of(10L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(MembershipCourseFactory.ERROR_COURSE_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Deduplication")
    class Deduplication {

        @Test
        @DisplayName("Should produce unique pairs only")
        void shouldProduceUniquePairsOnly() {
            // Given
            factory.setAvailableMembershipIds(List.of(1L, 2L));
            factory.setAvailableCourseIds(List.of(100L, 200L));

            // When
            List<MembershipCourseRecord> result = factory.generate(4);

            // Then
            long uniqueKeys = result.stream()
                    .map(r -> r.membershipId() + ":" + r.courseId())
                    .distinct().count();
            assertThat(uniqueKeys).isEqualTo(result.size());
        }
    }

    @Nested
    @DisplayName("Record population")
    class RecordPopulation {

        @Test
        @DisplayName("Should populate all fields when generating single record")
        void shouldPopulateAllFields_whenGeneratingSingleRecord() {
            // Given
            factory.setAvailableMembershipIds(List.of(10L));
            factory.setAvailableCourseIds(List.of(30L));

            // When
            List<MembershipCourseRecord> result = factory.generate(1);
            MembershipCourseRecord record = result.get(0);

            // Then
            assertThat(record.membershipId()).isEqualTo(10L);
            assertThat(record.courseId()).isEqualTo(30L);
        }
    }
}
