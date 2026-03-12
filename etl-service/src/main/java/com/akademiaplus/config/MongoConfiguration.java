/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration for the ETL system.
 *
 * <p>Creates indexes on startup for efficient querying of migration jobs and rows.</p>
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.akademiaplus.interfaceadapters")
public class MongoConfiguration {

    public static final String COLLECTION_JOBS = "migration_jobs";
    public static final String COLLECTION_ROWS = "migration_rows";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_JOB_ID = "jobId";
    public static final String FIELD_STATUS = "status";

    private final MongoTemplate mongoTemplate;

    /**
     * Creates a new MongoConfiguration.
     *
     * @param mongoTemplate the Spring Data MongoDB template for index management
     */
    public MongoConfiguration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Creates MongoDB indexes after the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        mongoTemplate.indexOps(COLLECTION_JOBS)
                .ensureIndex(new Index().on(FIELD_TENANT_ID, Sort.Direction.ASC));

        mongoTemplate.indexOps(COLLECTION_ROWS)
                .ensureIndex(new Index().on(FIELD_JOB_ID, Sort.Direction.ASC));

        mongoTemplate.indexOps(COLLECTION_ROWS)
                .ensureIndex(new Index()
                        .on(FIELD_JOB_ID, Sort.Direction.ASC)
                        .on(FIELD_STATUS, Sort.Direction.ASC));
    }
}
