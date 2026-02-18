/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Declares every entity in the mock data dependency graph.
 *
 * <p>Each value carries three properties:</p>
 * <ul>
 *   <li>{@code loadable} — {@code true} if a {@code DataLoader} and
 *       {@code AbstractMockDataUseCase} exist for this entity.</li>
 *   <li>{@code cleanable} — {@code true} if a {@code DataCleanUp} exists
 *       (currently all values are cleanable).</li>
 *   <li>{@code dependencies} — the direct FK parents in the graph;
 *       cleanup must delete a child <em>before</em> its parents.</li>
 * </ul>
 *
 * <p>People entities list auth/PII as parents (not TENANT directly)
 * so cleanup deletes people → auth/PII → TENANT in the correct FK order.
 * TENANT is reached transitively through auth/PII.</p>
 */
@Getter
public enum MockEntityType {

    // ── Level 0: root entity ──
    TENANT(true, true),

    // ── Level 0: cleanup-only shared tables (FK → TENANT) ──
    TENANT_SEQUENCE(false, true, TENANT),
    PERSON_PII(false, true, TENANT),
    INTERNAL_AUTH(false, true, TENANT),
    CUSTOMER_AUTH(false, true, TENANT),

    // ── Level 1: people with internal auth (FK → INTERNAL_AUTH, PERSON_PII) ──
    EMPLOYEE(true, true, INTERNAL_AUTH, PERSON_PII),
    COLLABORATOR(true, true, INTERNAL_AUTH, PERSON_PII),

    // ── Level 1: people with customer auth (FK → CUSTOMER_AUTH, PERSON_PII) ──
    ADULT_STUDENT(true, true, CUSTOMER_AUTH, PERSON_PII),
    TUTOR(true, true, CUSTOMER_AUTH, PERSON_PII),

    // ── Level 2: depends on TUTOR ──
    MINOR_STUDENT(true, true, TUTOR);

    private final boolean loadable;
    private final boolean cleanable;
    /**
     * -- GETTER --
     *  Returns the direct FK parents of this entity in the dependency graph.
     */
    private final Set<MockEntityType> dependencies;

    MockEntityType(boolean loadable, boolean cleanable, MockEntityType... dependencies) {
        this.loadable = loadable;
        this.cleanable = cleanable;
        this.dependencies = dependencies.length == 0
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new java.util.LinkedHashSet<>(Arrays.asList(dependencies)));
    }

}
