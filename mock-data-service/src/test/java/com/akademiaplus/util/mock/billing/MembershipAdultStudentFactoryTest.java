/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MembershipAdultStudentFactory}.
 */
@DisplayName("MembershipAdultStudentFactory")
class MembershipAdultStudentFactoryTest {

    private MembershipAdultStudentFactory factory;

    @BeforeEach
    void setUp() {
        factory = new MembershipAdultStudentFactory();
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and all IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndAllIdsSet() {
            // Given
            int expectedCount = 3;
            factory.setAvailableMembershipIds(List.of(10L));
            factory.setAvailableCourseIds(List.of(20L));
            factory.setAvailableAdultStudentIds(List.of(30L));

            // When
            List<MembershipAdultStudentCreationRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;
            factory.setAvailableMembershipIds(List.of(10L));
            factory.setAvailableCourseIds(List.of(20L));
            factory.setAvailableAdultStudentIds(List.of(30L));

            // When
            List<MembershipAdultStudentCreationRequestDTO> result = factory.generate(zeroCount);

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
            factory.setAvailableCourseIds(List.of(20L));
            factory.setAvailableAdultStudentIds(List.of(30L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(MembershipAdultStudentFactory.ERROR_MEMBERSHIP_IDS_NOT_SET);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when course IDs are not set")
        void shouldThrowIllegalStateException_whenCourseIdsNotSet() {
            // Given
            factory.setAvailableMembershipIds(List.of(10L));
            factory.setAvailableAdultStudentIds(List.of(30L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(MembershipAdultStudentFactory.ERROR_COURSE_IDS_NOT_SET);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when adult student IDs are not set")
        void shouldThrowIllegalStateException_whenAdultStudentIdsNotSet() {
            // Given
            factory.setAvailableMembershipIds(List.of(10L));
            factory.setAvailableCourseIds(List.of(20L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(MembershipAdultStudentFactory.ERROR_ADULT_STUDENT_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Round-robin assignment")
    class RoundRobinAssignment {

        @Test
        @DisplayName("Should assign IDs in round-robin fashion across all FK lists")
        void shouldAssignIdsInRoundRobinFashion() {
            // Given
            factory.setAvailableMembershipIds(List.of(100L, 200L));
            factory.setAvailableCourseIds(List.of(300L, 400L));
            factory.setAvailableAdultStudentIds(List.of(500L, 600L));

            // When
            List<MembershipAdultStudentCreationRequestDTO> result = factory.generate(4);

            // Then
            assertThat(result.get(0).getMembershipId()).isEqualTo(100L);
            assertThat(result.get(1).getMembershipId()).isEqualTo(200L);
            assertThat(result.get(2).getMembershipId()).isEqualTo(100L);
            assertThat(result.get(3).getMembershipId()).isEqualTo(200L);

            assertThat(result.get(0).getCourseId()).isEqualTo(300L);
            assertThat(result.get(1).getCourseId()).isEqualTo(400L);

            assertThat(result.get(0).getAdultStudentId()).isEqualTo(500L);
            assertThat(result.get(1).getAdultStudentId()).isEqualTo(600L);
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single association")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleAssociation() {
            // Given
            factory.setAvailableMembershipIds(List.of(10L));
            factory.setAvailableCourseIds(List.of(20L));
            factory.setAvailableAdultStudentIds(List.of(30L));

            // When
            List<MembershipAdultStudentCreationRequestDTO> result = factory.generate(1);
            MembershipAdultStudentCreationRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getStartDate()).isNotNull();
            assertThat(dto.getDueDate()).isNotNull();
            assertThat(dto.getDueDate()).isAfter(dto.getStartDate());
            assertThat(dto.getMembershipId()).isEqualTo(10L);
            assertThat(dto.getCourseId()).isEqualTo(20L);
            assertThat(dto.getAdultStudentId()).isEqualTo(30L);
        }
    }
}
