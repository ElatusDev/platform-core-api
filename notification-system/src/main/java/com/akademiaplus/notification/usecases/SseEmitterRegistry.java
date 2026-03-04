/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe registry for Server-Sent Event emitters.
 * <p>
 * Manages one {@link SseEmitter} per user per tenant using a composite key
 * of {@code "tenantId:userId"}. Registers lifecycle callbacks to automatically
 * remove emitters on completion, timeout, or error.
 */
@Service
public class SseEmitterRegistry {

    /** Default SSE connection timeout in milliseconds (5 minutes). */
    public static final long DEFAULT_TIMEOUT = 300_000L;

    /** Separator between tenantId and userId in the composite key. */
    public static final String EMITTER_KEY_SEPARATOR = ":";

    private final ConcurrentMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Creates and registers a new {@link SseEmitter} for the given tenant and user.
     * <p>
     * If an emitter already exists for this tenant-user pair, it is replaced.
     * Lifecycle callbacks are registered to auto-remove the emitter on
     * completion, timeout, or error.
     *
     * @param tenantId the tenant identifier
     * @param userId   the user identifier
     * @return the newly created SseEmitter
     */
    public SseEmitter register(Long tenantId, Long userId) {
        final String key = buildKey(tenantId, userId);
        final SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitter.onCompletion(() -> emitters.remove(key));
        emitter.onTimeout(() -> emitters.remove(key));
        emitter.onError(e -> emitters.remove(key));

        emitters.put(key, emitter);
        return emitter;
    }

    /**
     * Retrieves the active emitter for the given tenant and user.
     *
     * @param tenantId the tenant identifier
     * @param userId   the user identifier
     * @return the emitter if the user is connected, empty otherwise
     */
    public Optional<SseEmitter> getEmitter(Long tenantId, Long userId) {
        return Optional.ofNullable(emitters.get(buildKey(tenantId, userId)));
    }

    /**
     * Removes the emitter for the given tenant and user.
     *
     * @param tenantId the tenant identifier
     * @param userId   the user identifier
     */
    public void remove(Long tenantId, Long userId) {
        emitters.remove(buildKey(tenantId, userId));
    }

    /**
     * Builds the composite map key from tenant and user identifiers.
     *
     * @param tenantId the tenant identifier
     * @param userId   the user identifier
     * @return composite key in format "tenantId:userId"
     */
    String buildKey(Long tenantId, Long userId) {
        return tenantId + EMITTER_KEY_SEPARATOR + userId;
    }
}
