/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.config;

import com.akademiaplus.oauth.interfaceadapters.FacebookOAuthClient;
import com.akademiaplus.oauth.interfaceadapters.GoogleOAuthClient;
import com.akademiaplus.oauth.interfaceadapters.OAuthProviderClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Configuration for OAuth provider clients.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class OAuthConfiguration {

    /**
     * Provides a RestTemplate for OAuth provider HTTP calls.
     *
     * @return a default RestTemplate instance
     */
    @Bean
    public RestTemplate oauthRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Builds the provider client map keyed by provider name.
     *
     * @param googleClient   the Google OAuth client
     * @param facebookClient the Facebook OAuth client
     * @return map of provider name to client
     */
    @Bean
    public Map<String, OAuthProviderClient> providerClients(GoogleOAuthClient googleClient,
                                                              FacebookOAuthClient facebookClient) {
        return Map.of(
                "google", googleClient,
                "facebook", facebookClient
        );
    }
}
