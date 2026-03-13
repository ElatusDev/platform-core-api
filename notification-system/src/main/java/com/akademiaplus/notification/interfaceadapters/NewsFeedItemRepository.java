/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.newsfeed.NewsFeedItemDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsFeedItemRepository extends TenantScopedRepository<NewsFeedItemDataModel, NewsFeedItemDataModel.NewsFeedItemCompositeId> {

    /**
     * Finds published news feed items, optionally filtered by course.
     * Returns only items with status PUBLISHED, ordered by publishedAt descending.
     *
     * @param courseId optional course filter (null returns all published items)
     * @return published news feed items
     */
    @Query("SELECT n FROM NewsFeedItemDataModel n WHERE n.status = 'PUBLISHED' "
         + "AND n.deletedAt IS NULL "
         + "AND (:courseId IS NULL OR n.courseId = :courseId) "
         + "ORDER BY n.publishedAt DESC")
    List<NewsFeedItemDataModel> findPublished(@Param("courseId") Long courseId);
}
