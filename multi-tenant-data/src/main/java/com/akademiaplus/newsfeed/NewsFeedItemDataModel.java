/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.newsfeed;

import com.akademiaplus.infra.persistence.model.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing a news feed item in the notification module.
 * News feed items allow tenants to publish announcements, updates,
 * and educational content that can optionally be linked to a course.
 * <p>
 * Items follow a lifecycle: DRAFT → PUBLISHED → ARCHIVED.
 * Only published items are visible to end users in the news feed.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "news_feed_items")
@SQLDelete(sql = "UPDATE news_feed_items SET deleted_at = CURRENT_TIMESTAMP WHERE news_feed_item_id = ? AND tenant_id = ?")
@IdClass(NewsFeedItemDataModel.NewsFeedItemCompositeId.class)
public class NewsFeedItemDataModel extends TenantScoped {

    /**
     * Unique identifier for the news feed item within the tenant.
     * Auto-assigned per tenant by EntityIdAssigner.
     */
    @Id
    @Column(name = "news_feed_item_id")
    private Long newsFeedItemId;

    /**
     * Title of the news feed item displayed to users.
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * Rich content body of the news item.
     * Stored as TEXT to accommodate formatted or lengthy content.
     */
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * ID of the author (employee/collaborator) who created the item.
     */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /**
     * Optional course association for course-specific news.
     * Null indicates a general tenant-wide news item.
     */
    @Column(name = "course_id")
    private Long courseId;

    /**
     * Optional cover image URL for the news item.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Lifecycle status controlling visibility in the news feed.
     * Defaults to DRAFT on creation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NewsFeedStatus status;

    /**
     * Timestamp when the item was published.
     * Set automatically when status transitions to PUBLISHED.
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * Composite primary key class for NewsFeedItem entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NewsFeedItemCompositeId implements Serializable {
        private Long tenantId;
        private Long newsFeedItemId;
    }
}
