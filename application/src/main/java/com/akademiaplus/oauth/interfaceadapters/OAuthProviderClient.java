/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.interfaceadapters;

import com.akademiaplus.oauth.usecases.domain.OAuthUserProfile;

/**
 * Interface for exchanging an OAuth2 authorization code with a provider
 * and fetching the user's profile.
 *
 * @author ElatusDev
 * @since 1.0
 */
public interface OAuthProviderClient {

    /**
     * Exchanges an authorization code for a user profile.
     *
     * @param authorizationCode the authorization code from the provider consent flow
     * @param redirectUri       the redirect URI that was used during the consent flow
     * @return the user profile from the provider
     * @throws com.akademiaplus.oauth.exceptions.OAuthProviderException if the exchange fails
     */
    OAuthUserProfile exchangeCodeForProfile(String authorizationCode, String redirectUri);
}
