/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for creating and extracting HttpOnly cookies for JWT token delivery.
 *
 * <p>Manages access and refresh token cookies with Secure, HttpOnly,
 * and SameSite=Strict attributes. Supports cookie clearing for logout.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class CookieService {

    /** Path scope for access token cookies — available to all API endpoints. */
    public static final String ACCESS_TOKEN_PATH = "/api/v1";

    /** Path scope for refresh token cookies — only sent to token refresh endpoint. */
    public static final String REFRESH_TOKEN_PATH = "/api/v1/security/token";

    /** SameSite attribute value for CSRF protection. */
    public static final String SAME_SITE_STRICT = "Strict";

    /** HTTP header name for setting cookies. */
    public static final String SET_COOKIE_HEADER = "Set-Cookie";

    @Value("${security.cookie.domain}")
    private String cookieDomain;

    @Value("${security.cookie.secure}")
    private boolean secureCookie;

    @Value("${security.cookie.access-token-name}")
    private String accessTokenCookieName;

    @Value("${security.cookie.refresh-token-name}")
    private String refreshTokenCookieName;

    @Value("${security.cookie.access-token-max-age-seconds}")
    private long accessTokenMaxAge;

    @Value("${security.cookie.refresh-token-max-age-seconds}")
    private long refreshTokenMaxAge;

    /**
     * Adds access and refresh token cookies to the HTTP response.
     *
     * @param response     the HTTP response
     * @param accessToken  the JWT access token
     * @param refreshToken the JWT refresh token
     */
    public void addTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = buildCookie(accessTokenCookieName, accessToken, ACCESS_TOKEN_PATH, accessTokenMaxAge);
        ResponseCookie refreshCookie = buildCookie(refreshTokenCookieName, refreshToken, REFRESH_TOKEN_PATH, refreshTokenMaxAge);

        response.addHeader(SET_COOKIE_HEADER, accessCookie.toString());
        response.addHeader(SET_COOKIE_HEADER, refreshCookie.toString());
    }

    /**
     * Clears access and refresh token cookies by setting Max-Age to 0.
     *
     * @param response the HTTP response
     */
    public void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = buildCookie(accessTokenCookieName, "", ACCESS_TOKEN_PATH, 0);
        ResponseCookie refreshCookie = buildCookie(refreshTokenCookieName, "", REFRESH_TOKEN_PATH, 0);

        response.addHeader(SET_COOKIE_HEADER, accessCookie.toString());
        response.addHeader(SET_COOKIE_HEADER, refreshCookie.toString());
    }

    /**
     * Extracts the access token from the request cookies.
     *
     * @param request the HTTP request
     * @return the access token value, or empty if not present
     */
    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return extractCookieValue(request, accessTokenCookieName);
    }

    /**
     * Extracts the refresh token from the request cookies.
     *
     * @param request the HTTP request
     * @return the refresh token value, or empty if not present
     */
    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, refreshTokenCookieName);
    }

    private ResponseCookie buildCookie(String name, String value, String path, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(SAME_SITE_STRICT)
                .path(path)
                .domain(cookieDomain)
                .maxAge(maxAge)
                .build();
    }

    private Optional<String> extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
