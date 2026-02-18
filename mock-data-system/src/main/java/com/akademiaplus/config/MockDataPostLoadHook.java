/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

/**
 * Action executed after a specific entity type has been loaded.
 *
 * <p>Typical use: wiring generated IDs into a downstream factory.
 * For example, after tutors are loaded their IDs are collected and
 * injected into {@code MinorStudentFactory}.</p>
 */
@FunctionalInterface
public interface MockDataPostLoadHook {

    /**
     * Runs the post-load logic.
     */
    void execute();
}
