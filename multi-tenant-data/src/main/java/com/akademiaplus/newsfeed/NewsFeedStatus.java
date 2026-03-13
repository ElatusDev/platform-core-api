/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.newsfeed;

/**
 * Lifecycle status for news feed items.
 * Controls visibility and editorial flow of news items within the tenant.
 * <p>
 * Items transition through DRAFT → PUBLISHED → ARCHIVED.
 * Only PUBLISHED items are visible in public news feed queries.
 */
public enum NewsFeedStatus {
    /** Initial state; not visible to end users. */
    DRAFT,
    /** Visible to end users in the news feed. */
    PUBLISHED,
    /** Removed from the public feed but retained for historical reference. */
    ARCHIVED
}
