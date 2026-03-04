/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.notification.usecases.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Server-Sent Events endpoint for real-time notification delivery.
 * <p>
 * Clients subscribe via {@code GET /v1/notification-system/notifications/stream}
 * and receive push events when notifications are dispatched to them.
 * This controller is not OpenAPI-generated because SSE streaming
 * responses do not map well to OpenAPI's request-response model.
 */
@RestController
@RequestMapping("/v1/notification-system")
@RequiredArgsConstructor
public class SseController {

    /** JWT claim key for the user ID used as the SSE emitter key. */
    public static final String CLAIM_USER_ID = "user_id";

    /** Error when no authenticated user ID can be extracted from the security context. */
    public static final String ERROR_USER_ID_NOT_FOUND = "Authenticated user ID not found in security context";

    private final SseEmitterRegistry sseEmitterRegistry;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Opens an SSE connection for the authenticated user.
     * <p>
     * The emitter is registered in {@link SseEmitterRegistry} keyed by
     * the tenant and user IDs. The connection stays open until the client
     * disconnects, the emitter times out, or an error occurs.
     *
     * @return an SSE emitter that will push notification events
     */
    @GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        Long tenantId = tenantContextHolder.requireTenantId();
        Long userId = extractAuthenticatedUserId();
        return sseEmitterRegistry.register(tenantId, userId);
    }

    /**
     * Extracts the user ID from the current security context.
     * <p>
     * Follows the same pattern as {@code StoreTransactionCreationUseCase.extractEmployeeId()}.
     * Expects the user ID to be stored as a custom claim ({@value CLAIM_USER_ID})
     * in the JWT token's details map.
     * <p>
     * <strong>IMPORTANT:</strong> The {@value CLAIM_USER_ID} claim must be added to
     * the JWT token during authentication. If the JWT structure uses a different
     * claim name for the user's numeric ID, update {@link #CLAIM_USER_ID} accordingly.
     *
     * @return the authenticated user's numeric ID
     * @throws IllegalStateException if the user ID cannot be extracted
     */
    Long extractAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token) {
            Object details = token.getDetails();
            if (details instanceof Map<?, ?> claims) {
                Object userIdClaim = claims.get(CLAIM_USER_ID);
                if (userIdClaim != null) {
                    return Long.valueOf(userIdClaim.toString());
                }
            }
        }
        throw new IllegalStateException(ERROR_USER_ID_NOT_FOUND);
    }
}
