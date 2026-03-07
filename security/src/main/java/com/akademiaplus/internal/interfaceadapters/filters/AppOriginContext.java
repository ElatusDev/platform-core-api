/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.filters;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for reading and writing the app origin on HTTP request attributes.
 *
 * <p>The app origin identifies whether a request comes from the
 * akademia-plus-web application (school premises, IP whitelist) or
 * the elatusdev-web application (public internet, full security).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
public final class AppOriginContext {

    /** Request attribute key for the resolved app origin. */
    public static final String APP_ORIGIN_ATTRIBUTE = "com.akademiaplus.appOrigin";

    /** HTTP header for explicit app origin identification. */
    public static final String APP_ORIGIN_HEADER = "X-App-Origin";

    /** App origin value for akademia-plus-web. */
    public static final String ORIGIN_AKADEMIA = "akademia";

    /** App origin value for elatusdev-web. */
    public static final String ORIGIN_ELATUS = "elatus";

    /** Path prefix for akademia-plus-web requests. */
    public static final String PATH_PREFIX_AKADEMIA = "/akademia/";

    /** Path prefix for elatusdev-web requests. */
    public static final String PATH_PREFIX_ELATUS = "/elatus/";

    /** Default origin (fail-secure). */
    public static final String DEFAULT_ORIGIN = ORIGIN_ELATUS;

    private AppOriginContext() {
        // Utility class
    }

    /**
     * Sets the app origin on the request attributes.
     *
     * @param request   the HTTP request
     * @param appOrigin the resolved app origin
     */
    public static void setAppOrigin(HttpServletRequest request, String appOrigin) {
        request.setAttribute(APP_ORIGIN_ATTRIBUTE, appOrigin);
    }

    /**
     * Retrieves the app origin from the request attributes.
     *
     * @param request the HTTP request
     * @return the app origin, or the default (elatus) if not set
     */
    public static String getAppOrigin(HttpServletRequest request) {
        Object origin = request.getAttribute(APP_ORIGIN_ATTRIBUTE);
        return (origin instanceof String s) ? s : DEFAULT_ORIGIN;
    }

    /**
     * Checks whether the request originates from akademia-plus-web.
     *
     * @param request the HTTP request
     * @return true if the app origin is akademia
     */
    public static boolean isAkademia(HttpServletRequest request) {
        return ORIGIN_AKADEMIA.equals(getAppOrigin(request));
    }

    /**
     * Checks whether the request originates from elatusdev-web.
     *
     * @param request the HTTP request
     * @return true if the app origin is elatus
     */
    public static boolean isElatus(HttpServletRequest request) {
        return ORIGIN_ELATUS.equals(getAppOrigin(request));
    }
}
