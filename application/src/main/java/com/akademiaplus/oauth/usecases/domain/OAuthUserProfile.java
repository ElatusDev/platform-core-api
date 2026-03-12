/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.usecases.domain;

/**
 * User profile retrieved from the OAuth provider after token exchange.
 *
 * @param providerUserId the user's unique identifier from the provider
 * @param email          the user's email address
 * @param name           the user's display name (may be null)
 * @author ElatusDev
 * @since 1.0
 */
public record OAuthUserProfile(String providerUserId, String email, String name) {
}
