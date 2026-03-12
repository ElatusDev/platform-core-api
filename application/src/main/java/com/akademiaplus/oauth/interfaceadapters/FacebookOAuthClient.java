/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.interfaceadapters;

import com.akademiaplus.oauth.exceptions.OAuthProviderException;
import com.akademiaplus.oauth.usecases.domain.OAuthUserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Facebook OAuth2 provider client.
 *
 * <p>Exchanges an authorization code for an access token via Facebook's token
 * endpoint, then fetches the user's profile from the Graph API.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class FacebookOAuthClient implements OAuthProviderClient {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookOAuthClient.class);
    private static final String PROVIDER = "facebook";
    private static final String TOKEN_URL = "https://graph.facebook.com/v19.0/oauth/access_token";
    private static final String USERINFO_URL = "https://graph.facebook.com/v19.0/me";

    private final RestTemplate restTemplate;

    @Value("${oauth.facebook.client-id:}")
    private String clientId;

    @Value("${oauth.facebook.client-secret:}")
    private String clientSecret;

    /**
     * Constructs the client with a RestTemplate.
     *
     * @param restTemplate the REST template for HTTP calls
     */
    public FacebookOAuthClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public OAuthUserProfile exchangeCodeForProfile(String authorizationCode, String redirectUri) {
        String accessToken = exchangeCodeForToken(authorizationCode, redirectUri);
        return fetchUserProfile(accessToken);
    }

    private String exchangeCodeForToken(String authorizationCode, String redirectUri) {
        try {
            String url = UriComponentsBuilder.fromUriString(TOKEN_URL)
                    .queryParam("client_id", clientId)
                    .queryParam("client_secret", clientSecret)
                    .queryParam("redirect_uri", redirectUri)
                    .queryParam("code", authorizationCode)
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new OAuthProviderException(PROVIDER);
            }

            return (String) response.get("access_token");
        } catch (RestClientException e) {
            LOG.error("Facebook token exchange failed: {}", e.getMessage());
            throw new OAuthProviderException(PROVIDER, e);
        }
    }

    private OAuthUserProfile fetchUserProfile(String accessToken) {
        try {
            String url = UriComponentsBuilder.fromUriString(USERINFO_URL)
                    .queryParam("fields", "id,name,email")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("email")) {
                throw new OAuthProviderException(PROVIDER);
            }

            return new OAuthUserProfile(
                    (String) response.get("id"),
                    (String) response.get("email"),
                    (String) response.get("name")
            );
        } catch (RestClientException e) {
            LOG.error("Facebook userinfo fetch failed: {}", e.getMessage());
            throw new OAuthProviderException(PROVIDER, e);
        }
    }
}
