/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.newsfeed.NewsFeedItemDataModel;
import com.akademiaplus.newsfeed.NewsFeedStatus;
import com.akademiaplus.util.base.DataFactory;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Factory for creating {@link NewsFeedItemDataModel} instances with fake data.
 *
 * <p>Requires employee IDs to be injected via setter before
 * {@link #generate(int)} is called. Course IDs are optional; when provided,
 * approximately 50% of generated items will be linked to a course.</p>
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class NewsFeedItemFactory implements DataFactory<NewsFeedItemDataModel> {

    /** Error message when employee IDs have not been set. */
    public static final String ERROR_EMPLOYEE_IDS_NOT_SET =
            "availableEmployeeIds must be set before generating news feed items";

    private static final double COURSE_ASSIGNMENT_PROBABILITY = 0.5;
    private static final int MAX_PUBLISHED_DAYS_AGO = 30;

    private final ApplicationContext applicationContext;
    private final NewsFeedItemDataGenerator generator;
    private final Random random;

    @Setter
    private List<Long> availableEmployeeIds = List.of();

    @Setter
    private List<Long> availableCourseIds = List.of();

    /**
     * Constructs the factory with the application context and data generator.
     *
     * @param applicationContext the Spring application context for prototype bean creation
     * @param generator          the news feed item data generator
     */
    public NewsFeedItemFactory(ApplicationContext applicationContext,
                               NewsFeedItemDataGenerator generator) {
        this.applicationContext = applicationContext;
        this.generator = generator;
        this.random = new Random();
    }

    /**
     * Generates the specified number of {@link NewsFeedItemDataModel} instances.
     *
     * @param count the number of news feed items to generate
     * @return a list of generated news feed item data models
     * @throws IllegalStateException if employee IDs have not been set
     */
    @Override
    public List<NewsFeedItemDataModel> generate(int count) {
        if (availableEmployeeIds.isEmpty()) {
            throw new IllegalStateException(ERROR_EMPLOYEE_IDS_NOT_SET);
        }

        List<NewsFeedItemDataModel> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(createNewsFeedItem(i));
        }
        return items;
    }

    private NewsFeedItemDataModel createNewsFeedItem(int index) {
        NewsFeedItemDataModel model = applicationContext.getBean(NewsFeedItemDataModel.class);
        Long authorId = availableEmployeeIds.get(index % availableEmployeeIds.size());

        NewsFeedStatus status = generator.status();

        model.setTitle(generator.title());
        model.setBody(generator.body());
        model.setAuthorId(authorId);
        model.setCourseId(resolveCourseId(index));
        model.setImageUrl(generator.imageUrl());
        model.setStatus(status);
        model.setPublishedAt(resolvePublishedAt(status));
        return model;
    }

    private Long resolveCourseId(int index) {
        if (availableCourseIds.isEmpty() || random.nextDouble() < COURSE_ASSIGNMENT_PROBABILITY) {
            return null;
        }
        return availableCourseIds.get(index % availableCourseIds.size());
    }

    private LocalDateTime resolvePublishedAt(NewsFeedStatus status) {
        if (status == NewsFeedStatus.PUBLISHED) {
            int daysAgo = random.nextInt(MAX_PUBLISHED_DAYS_AGO) + 1;
            return LocalDateTime.now().minusDays(daysAgo);
        }
        return null;
    }
}
