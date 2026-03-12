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
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Google OAuth2 provider client.
 *
 * <p>Exchanges an authorization code for tokens via Google's token endpoint,
 * then fetches the user's profile from the userinfo endpoint.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class GoogleOAuthClient implements OAuthProviderClient {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleOAuthClient.class);
    private static final String PROVIDER = "google";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestTemplate restTemplate;

    @Value("${oauth.google.client-id:}")
    private String clientId;

    @Value("${oauth.google.client-secret:}")
    private String clientSecret;

    /**
     * Constructs the client with a RestTemplate.
     *
     * @param restTemplate the REST template for HTTP calls
     */
    public GoogleOAuthClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public OAuthUserProfile exchangeCodeForProfile(String authorizationCode, String redirectUri) {
        String accessToken = exchangeCodeForToken(authorizationCode, redirectUri);
        return fetchUserProfile(accessToken);
    }

    private String exchangeCodeForToken(String authorizationCode, String redirectUri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("code", authorizationCode);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("redirect_uri", redirectUri);
            body.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(TOKEN_URL, request, Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new OAuthProviderException(PROVIDER);
            }

            return (String) response.get("access_token");
        } catch (RestClientException e) {
            LOG.error("Google token exchange failed: {}", e.getMessage());
            throw new OAuthProviderException(PROVIDER, e);
        }
    }

    private OAuthUserProfile fetchUserProfile(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    USERINFO_URL, HttpMethod.GET, request, Map.class
            ).getBody();

            if (response == null || !response.containsKey("email")) {
                throw new OAuthProviderException(PROVIDER);
            }

            return new OAuthUserProfile(
                    (String) response.get("sub"),
                    (String) response.get("email"),
                    (String) response.get("name")
            );
        } catch (RestClientException e) {
            LOG.error("Google userinfo fetch failed: {}", e.getMessage());
            throw new OAuthProviderException(PROVIDER, e);
        }
    }
}
