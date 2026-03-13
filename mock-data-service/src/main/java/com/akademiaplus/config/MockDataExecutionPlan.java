/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Computes FK-safe load and cleanup ordering for a set of mock entities.
 *
 * <p>Given a set of requested entities, the plan:</p>
 * <ol>
 *   <li>Expands the set to its <em>transitive closure</em> (all recursive dependencies).</li>
 *   <li>Runs Kahn's topological sort on the closure to produce a {@code loadOrder}
 *       containing only {@linkplain MockEntityType#isLoadable() loadable} entities.</li>
 *   <li>Reverses the full closure to produce a {@code cleanupOrder}
 *       containing only {@linkplain MockEntityType#isCleanable() cleanable} entities.</li>
 * </ol>
 */
public class MockDataExecutionPlan {

    static final String ERROR_NULL_ENTITY_SET = "Requested entity set must not be null";

    private final List<MockEntityType> loadOrder;
    private final List<MockEntityType> cleanupOrder;

    private MockDataExecutionPlan(List<MockEntityType> loadOrder,
                                  List<MockEntityType> cleanupOrder) {
        this.loadOrder = Collections.unmodifiableList(loadOrder);
        this.cleanupOrder = Collections.unmodifiableList(cleanupOrder);
    }

    /**
     * Creates a plan that includes every entity in the graph.
     *
     * @return execution plan covering all enum values
     */
    public static MockDataExecutionPlan forAll() {
        return forEntities(EnumSet.allOf(MockEntityType.class));
    }

    /**
     * Creates a plan for the given entities plus all their transitive dependencies.
     *
     * @param requested the entities the caller wants to load; must not be {@code null}
     * @return execution plan with FK-safe ordering
     * @throws IllegalArgumentException if {@code requested} is {@code null}
     */
    public static MockDataExecutionPlan forEntities(Set<MockEntityType> requested) {
        if (requested == null) {
            throw new IllegalArgumentException(ERROR_NULL_ENTITY_SET);
        }
        if (requested.isEmpty()) {
            return new MockDataExecutionPlan(List.of(), List.of());
        }

        Set<MockEntityType> closure = computeTransitiveClosure(requested);
        List<MockEntityType> sorted = topologicalSort(closure);

        List<MockEntityType> loadOrder = sorted.stream()
                .filter(MockEntityType::isLoadable)
                .toList();

        List<MockEntityType> cleanupOrder = sorted.reversed().stream()
                .filter(MockEntityType::isCleanable)
                .toList();

        return new MockDataExecutionPlan(loadOrder, cleanupOrder);
    }

    public List<MockEntityType> getLoadOrder() {
        return loadOrder;
    }

    public List<MockEntityType> getCleanupOrder() {
        return cleanupOrder;
    }

    /**
     * Expands the requested set by recursively collecting all dependencies.
     */
    private static Set<MockEntityType> computeTransitiveClosure(Set<MockEntityType> requested) {
        Set<MockEntityType> closure = EnumSet.noneOf(MockEntityType.class);
        Queue<MockEntityType> queue = new ArrayDeque<>(requested);
        while (!queue.isEmpty()) {
            MockEntityType current = queue.poll();
            if (closure.add(current)) {
                queue.addAll(current.getDependencies());
            }
        }
        return closure;
    }

    /**
     * Kahn's algorithm: produces a topological ordering where parents precede children.
     *
     * <p>In-degree is computed only within the closure set so entities outside
     * the closure do not affect the result.</p>
     */
    private static List<MockEntityType> topologicalSort(Set<MockEntityType> closure) {
        Map<MockEntityType, Integer> inDegree = new EnumMap<>(MockEntityType.class);
        for (MockEntityType entity : closure) {
            inDegree.putIfAbsent(entity, 0);
            for (MockEntityType dep : entity.getDependencies()) {
                if (closure.contains(dep)) {
                    inDegree.merge(entity, 1, Integer::sum);
                }
            }
        }

        Queue<MockEntityType> ready = new ArrayDeque<>();
        for (MockEntityType entity : closure) {
            if (inDegree.get(entity) == 0) {
                ready.add(entity);
            }
        }

        List<MockEntityType> sorted = new ArrayList<>(closure.size());
        while (!ready.isEmpty()) {
            MockEntityType current = ready.poll();
            sorted.add(current);
            for (MockEntityType entity : closure) {
                if (entity.getDependencies().contains(current)) {
                    int updated = inDegree.merge(entity, -1, Integer::sum);
                    if (updated == 0) {
                        ready.add(entity);
                    }
                }
            }
        }

        return sorted;
    }
}
