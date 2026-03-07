/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Interface for modules to contribute their security rules to the
 * application's {@link org.springframework.security.web.SecurityFilterChain}.
 *
 * <p>Modules implement this interface to declare which endpoints they
 * expose and what authorization rules apply. The {@link SecurityConfig}
 * collects all implementations and applies them to the filter chain.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
public interface ModuleSecurityConfigurator {

    /**
     * Configures authorization rules for this module's endpoints.
     *
     * <p>Called for both the akademia and elatus security filter chains.
     * Use this method for rules that apply regardless of app origin.</p>
     *
     * @param auth the authorization configuration registry
     * @throws Exception if configuration fails
     */
    void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) throws Exception;

    /**
     * Configures authorization rules specific to a given app origin.
     *
     * <p>Override this method to apply different rules for akademia-plus-web
     * vs elatusdev-web. The default implementation delegates to
     * {@link #configure(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry)}.</p>
     *
     * @param auth      the authorization configuration registry
     * @param appOrigin the app origin ("akademia" or "elatus")
     * @throws Exception if configuration fails
     */
    default void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
                           String appOrigin) throws Exception {
        configure(auth);
    }
}
