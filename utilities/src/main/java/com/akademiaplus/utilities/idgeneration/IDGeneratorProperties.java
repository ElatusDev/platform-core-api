package com.akademiaplus.utilities.idgeneration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for ID Generator Service
 * Can be configured in application.yml
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "id-generator")
public class IDGeneratorProperties {

    /**
     * Default batch size for ID allocation
     */
    private int defaultBatchSize = 20;

    /**
     * Maximum batch size allowed
     */
    private int maxBatchSize = 1000;

    /**
     * Enable automatic table creation
     */
    private boolean autoCreateTable = true;

    /**
     * Maximum number of retry attempts for optimistic locking
     */
    private int maxRetryAttempts = 5;

    /**
     * Initial retry delay in milliseconds
     */
    private int retryDelayMs = 50;

    /**
     * Retry delay multiplier for exponential backoff
     */
    private double retryMultiplier = 2.0;
}