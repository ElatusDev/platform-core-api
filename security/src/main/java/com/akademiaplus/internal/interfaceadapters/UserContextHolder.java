/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Holds the current user context using a thread-local variable.
 *
 * <p>The user context (profile type and profile ID) is set by the
 * {@link com.akademiaplus.internal.interfaceadapters.filters.UserContextLoader}
 * filter from JWT claims. Self-service endpoints ({@code /v1/my/*}) use this
 * holder to access the authenticated customer's profile identity.</p>
 *
 * <p>Uses {@code ThreadLocal} so the same value is visible to all components
 * on the current thread, regardless of which bean instance they hold.
 * Follows the same pattern as
 * {@link com.akademiaplus.infra.persistence.config.TenantContextHolder}.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class UserContextHolder {

    /** Error message when profile ID is required but missing. */
    public static final String ERROR_PROFILE_ID_REQUIRED = "User profile ID is required but not set in context";

    /** Error message when profile type is required but missing. */
    public static final String ERROR_PROFILE_TYPE_REQUIRED = "User profile type is required but not set in context";

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    /**
     * Immutable record holding the authenticated customer's profile identity.
     *
     * @param profileType the profile type ({@code ADULT_STUDENT} or {@code TUTOR})
     * @param profileId   the profile entity ID (adultStudentId or tutorId)
     */
    public record UserContext(String profileType, Long profileId) {}

    /**
     * Sets the user context for this thread.
     *
     * @param profileType the profile type
     * @param profileId   the profile entity ID
     */
    public void set(String profileType, Long profileId) {
        CONTEXT.set(new UserContext(profileType, profileId));
    }

    /**
     * Returns the current user context, if set.
     *
     * @return optional containing the user context, or empty if not set
     */
    public Optional<UserContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    /**
     * Returns the current profile ID or throws {@link IllegalStateException}
     * if no user context is set.
     *
     * @return the current profile ID, never null
     * @throws IllegalStateException if user context is missing
     */
    public Long requireProfileId() {
        return get()
                .map(UserContext::profileId)
                .orElseThrow(() -> new IllegalStateException(ERROR_PROFILE_ID_REQUIRED));
    }

    /**
     * Returns the current profile type or throws {@link IllegalStateException}
     * if no user context is set.
     *
     * @return the current profile type, never null
     * @throws IllegalStateException if user context is missing
     */
    public String requireProfileType() {
        return get()
                .map(UserContext::profileType)
                .orElseThrow(() -> new IllegalStateException(ERROR_PROFILE_TYPE_REQUIRED));
    }

    /**
     * Clears the user context for this thread.
     */
    public void clear() {
        CONTEXT.remove();
    }
}
