package com.akademiaplus.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;

import static com.akademiaplus.config.MockEntityType.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockEntityType")
class MockEntityTypeTest {

    @Nested
    @DisplayName("Graph integrity")
    class GraphIntegrity {

        @Test
        @DisplayName("Should have no circular dependencies in the graph")
        void shouldHaveNoCircularDependencies_inTheGraph() {
            // Given
            Set<MockEntityType> visited = EnumSet.noneOf(MockEntityType.class);
            Set<MockEntityType> inStack = EnumSet.noneOf(MockEntityType.class);

            // When & Then — DFS cycle detection for every node
            for (MockEntityType entity : values()) {
                assertThat(hasCycle(entity, visited, inStack))
                        .as("Cycle detected involving %s", entity)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("Should reach TENANT transitively from every loadable entity")
        void shouldReachTenantTransitively_fromEveryLoadableEntity() {
            // Given
            Set<MockEntityType> loadableEntities = EnumSet.noneOf(MockEntityType.class);
            for (MockEntityType entity : values()) {
                if (entity.isLoadable() && entity != TENANT) {
                    loadableEntities.add(entity);
                }
            }

            // When & Then
            for (MockEntityType entity : loadableEntities) {
                assertThat(transitiveClosure(entity))
                        .as("%s should transitively depend on TENANT", entity)
                        .contains(TENANT);
            }
        }

        @Test
        @DisplayName("Should reach TUTOR transitively from MINOR_STUDENT")
        void shouldReachTutorTransitively_fromMinorStudent() {
            // Given & When
            Set<MockEntityType> closure = transitiveClosure(MINOR_STUDENT);

            // Then
            assertThat(closure).contains(TUTOR);
        }
    }

    @Nested
    @DisplayName("Entity properties")
    class EntityProperties {

        @Test
        @DisplayName("Should mark TENANT as loadable")
        void shouldMarkTenantAsLoadable() {
            assertThat(TENANT.isLoadable()).isTrue();
        }

        @Test
        @DisplayName("Should mark TENANT_SEQUENCE as not loadable")
        void shouldMarkTenantSequenceAsNotLoadable() {
            assertThat(TENANT_SEQUENCE.isLoadable()).isFalse();
        }

        @Test
        @DisplayName("Should mark all enum values as cleanable")
        void shouldMarkAllEnumValuesAsCleanable() {
            for (MockEntityType entity : values()) {
                assertThat(entity.isCleanable())
                        .as("%s should be cleanable", entity)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should have no dependencies for TENANT")
        void shouldHaveNoDependencies_forTenant() {
            assertThat(TENANT.getDependencies()).isEmpty();
        }

        @Test
        @DisplayName("Should have TUTOR as direct dependency for MINOR_STUDENT")
        void shouldHaveTutorAsDirectDependency_forMinorStudent() {
            assertThat(MINOR_STUDENT.getDependencies()).containsExactly(TUTOR);
        }
    }

    // ── Helper methods ──

    private boolean hasCycle(MockEntityType node,
                             Set<MockEntityType> visited,
                             Set<MockEntityType> inStack) {
        if (inStack.contains(node)) {
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }
        visited.add(node);
        inStack.add(node);
        for (MockEntityType dep : node.getDependencies()) {
            if (hasCycle(dep, visited, inStack)) {
                return true;
            }
        }
        inStack.remove(node);
        return false;
    }

    private Set<MockEntityType> transitiveClosure(MockEntityType start) {
        Set<MockEntityType> closure = EnumSet.noneOf(MockEntityType.class);
        Queue<MockEntityType> queue = new ArrayDeque<>(start.getDependencies());
        while (!queue.isEmpty()) {
            MockEntityType current = queue.poll();
            if (closure.add(current)) {
                queue.addAll(current.getDependencies());
            }
        }
        return closure;
    }
}
