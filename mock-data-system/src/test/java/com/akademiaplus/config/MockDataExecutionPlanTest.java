package com.akademiaplus.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.akademiaplus.config.MockEntityType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MockDataExecutionPlan")
class MockDataExecutionPlanTest {

    @Nested
    @DisplayName("forAll()")
    class ForAll {

        private final MockDataExecutionPlan plan = MockDataExecutionPlan.forAll();

        @Test
        @DisplayName("Should place TENANT first in load order")
        void shouldPlaceTenantFirst_inLoadOrder() {
            assertThat(plan.getLoadOrder().get(0)).isEqualTo(TENANT);
        }

        @Test
        @DisplayName("Should place TENANT last in cleanup order")
        void shouldPlaceTenantLast_inCleanupOrder() {
            List<MockEntityType> cleanup = plan.getCleanupOrder();
            assertThat(cleanup.get(cleanup.size() - 1)).isEqualTo(TENANT);
        }

        @Test
        @DisplayName("Should place MINOR_STUDENT after TUTOR in load order")
        void shouldPlaceMinorStudentAfterTutor_inLoadOrder() {
            List<MockEntityType> load = plan.getLoadOrder();
            assertThat(load.indexOf(TUTOR))
                    .as("TUTOR should precede MINOR_STUDENT")
                    .isLessThan(load.indexOf(MINOR_STUDENT));
        }

        @Test
        @DisplayName("Should place shared tables between people and TENANT in cleanup order")
        void shouldPlaceSharedTablesBetweenPeopleAndTenant_inCleanupOrder() {
            // Given
            List<MockEntityType> cleanup = plan.getCleanupOrder();
            int minorStudentIdx = cleanup.indexOf(MINOR_STUDENT);
            int tenantIdx = cleanup.indexOf(TENANT);
            int personPiiIdx = cleanup.indexOf(PERSON_PII);
            int internalAuthIdx = cleanup.indexOf(INTERNAL_AUTH);
            int customerAuthIdx = cleanup.indexOf(CUSTOMER_AUTH);
            int tenantSequenceIdx = cleanup.indexOf(TENANT_SEQUENCE);

            // Then — shared tables sit between leaf entities and TENANT
            assertThat(personPiiIdx).isBetween(minorStudentIdx + 1, tenantIdx - 1);
            assertThat(internalAuthIdx).isBetween(minorStudentIdx + 1, tenantIdx - 1);
            assertThat(customerAuthIdx).isBetween(minorStudentIdx + 1, tenantIdx - 1);
            assertThat(tenantSequenceIdx).isBetween(minorStudentIdx + 1, tenantIdx - 1);
        }

        @Test
        @DisplayName("Should include only loadable entities in load order")
        void shouldIncludeOnlyLoadableEntities_inLoadOrder() {
            for (MockEntityType entity : plan.getLoadOrder()) {
                assertThat(entity.isLoadable())
                        .as("%s should be loadable", entity)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should include only cleanable entities in cleanup order")
        void shouldIncludeOnlyCleanableEntities_inCleanupOrder() {
            for (MockEntityType entity : plan.getCleanupOrder()) {
                assertThat(entity.isCleanable())
                        .as("%s should be cleanable", entity)
                        .isTrue();
            }
        }
    }

    @Nested
    @DisplayName("forEntities() — subset selection")
    class ForEntities {

        @Test
        @DisplayName("Should include transitive closure for MINOR_STUDENT")
        void shouldIncludeTransitiveClosure_forMinorStudent() {
            // Given
            Set<MockEntityType> requested = EnumSet.of(MINOR_STUDENT);

            // When
            MockDataExecutionPlan plan = MockDataExecutionPlan.forEntities(requested);

            // Then — closure must include TUTOR, CUSTOMER_AUTH, PERSON_PII, TENANT
            assertThat(plan.getLoadOrder()).contains(TENANT, TUTOR, MINOR_STUDENT);
            assertThat(plan.getCleanupOrder()).contains(TENANT, TUTOR, MINOR_STUDENT,
                    CUSTOMER_AUTH, PERSON_PII);
        }

        @Test
        @DisplayName("Should include INTERNAL_AUTH and PERSON_PII for EMPLOYEE")
        void shouldIncludeInternalAuthAndPersonPii_forEmployee() {
            // Given
            Set<MockEntityType> requested = EnumSet.of(EMPLOYEE);

            // When
            MockDataExecutionPlan plan = MockDataExecutionPlan.forEntities(requested);

            // Then
            assertThat(plan.getLoadOrder()).contains(TENANT, EMPLOYEE);
            assertThat(plan.getCleanupOrder()).contains(INTERNAL_AUTH, PERSON_PII, TENANT);
        }

        @Test
        @DisplayName("Should return empty plans when given empty set")
        void shouldReturnEmptyPlans_whenGivenEmptySet() {
            // Given
            Set<MockEntityType> empty = EnumSet.noneOf(MockEntityType.class);

            // When
            MockDataExecutionPlan plan = MockDataExecutionPlan.forEntities(empty);

            // Then
            assertThat(plan.getLoadOrder()).isEmpty();
            assertThat(plan.getCleanupOrder()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Should throw exception when given null set")
        void shouldThrowException_whenGivenNullSet() {
            assertThatThrownBy(() -> MockDataExecutionPlan.forEntities(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("Should return immutable load order")
        void shouldReturnImmutableLoadOrder() {
            // Given
            MockDataExecutionPlan plan = MockDataExecutionPlan.forAll();

            // When & Then
            assertThatThrownBy(() -> plan.getLoadOrder().add(TENANT))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should return immutable cleanup order")
        void shouldReturnImmutableCleanupOrder() {
            // Given
            MockDataExecutionPlan plan = MockDataExecutionPlan.forAll();

            // When & Then
            assertThatThrownBy(() -> plan.getCleanupOrder().add(TENANT))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
