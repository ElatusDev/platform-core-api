/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.notification;

import com.akademiaplus.newsfeed.NewsFeedItemDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock news feed item records into the database.
 */
@Service
public class LoadNewsFeedItemMockDataUseCase
        extends AbstractMockDataUseCase<NewsFeedItemDataModel, NewsFeedItemDataModel, NewsFeedItemDataModel.NewsFeedItemCompositeId> {

    /**
     * Creates a new use case with the required data loader and cleanup.
     *
     * @param dataLoader  the data loader for news feed item records
     * @param dataCleanUp the data cleanup for the news_feed_items table
     */
    public LoadNewsFeedItemMockDataUseCase(
            DataLoader<NewsFeedItemDataModel, NewsFeedItemDataModel, NewsFeedItemDataModel.NewsFeedItemCompositeId> dataLoader,
            @Qualifier("newsFeedItemDataCleanUp")
            DataCleanUp<NewsFeedItemDataModel, NewsFeedItemDataModel.NewsFeedItemCompositeId> dataCleanUp) {
        super(dataLoader, dataCleanUp);
    }
}
