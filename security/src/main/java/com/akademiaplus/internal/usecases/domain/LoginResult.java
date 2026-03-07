/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases.domain;

/**
 * Result of a successful login operation containing the issued token pair.
 *
 * @param accessToken  the JWT access token
 * @param refreshToken the JWT refresh token
 * @param username     the authenticated username
 * @author ElatusDev
 * @since 1.0
 */
public record LoginResult(String accessToken, String refreshToken, String username) {
}
