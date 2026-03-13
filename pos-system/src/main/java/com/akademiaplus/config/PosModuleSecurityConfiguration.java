/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.utilities.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@PropertySource("classpath:pos.properties")
@Configuration
public class PosModuleSecurityConfiguration implements ModuleSecurityConfigurator {

    private final String storeProductPath;
    private final String storeProductPathAnyPathVars;
    private final String storeTransactionPath;
    private final String storeTransactionPathAnyPathVars;
    private final String storeCatalogPath;
    private final String storeCatalogPathAnyPathVars;

    public PosModuleSecurityConfiguration(@Value("${api.pos.store-product.base-url}") String storeProductBaseUri,
                                          @Value("${api.pos.store-transaction.base-url}") String storeTransactionBaseUri,
                                          @Value("${api.pos.store-catalog.base-url}") String storeCatalogBaseUri) {
        String anyPathVar = "/**";
        storeProductPath = storeProductBaseUri;
        storeProductPathAnyPathVars = storeProductBaseUri + anyPathVar;
        storeTransactionPath = storeTransactionBaseUri;
        storeTransactionPathAnyPathVars = storeTransactionBaseUri + anyPathVar;
        storeCatalogPath = storeCatalogBaseUri;
        storeCatalogPathAnyPathVars = storeCatalogBaseUri + anyPathVar;
    }

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) throws Exception {
        storeCatalogPaths(auth);
        storeProductPaths(auth);
        storeTransactionPaths(auth);
    }

    private void storeCatalogPaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.GET, storeCatalogPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name(),
                        Role.COLLABORATOR.name(), Role.USER.name())
                .requestMatchers(HttpMethod.GET, storeCatalogPath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name(),
                        Role.COLLABORATOR.name(), Role.USER.name());
    }

    private void storeProductPaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.DELETE, storeProductPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.GET, storeProductPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.POST, storeProductPath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.PUT, storeProductPathAnyPathVars)
                .denyAll()
                .requestMatchers(HttpMethod.PATCH, storeProductPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.GET, storeProductPath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name());
    }

    private void storeTransactionPaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.DELETE, storeTransactionPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.GET, storeTransactionPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.POST, storeTransactionPath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.PUT, storeTransactionPathAnyPathVars)
                .denyAll()
                .requestMatchers(HttpMethod.PATCH, storeTransactionPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.GET, storeTransactionPath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name());
    }
}
