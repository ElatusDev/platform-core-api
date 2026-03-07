/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.usecases;

import com.akademiaplus.tokenbinding.usecases.domain.AnomalyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logs suspicious patterns detected during token binding verification.
 *
 * <p>This is a logging-only service for the initial implementation.
 * Future iterations may integrate with geo-IP services or push events
 * to a SIEM/alerting system.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class AnomalyDetectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnomalyDetectionService.class);

    /** Structured log template for anomaly events. */
    public static final String LOG_TEMPLATE_ANOMALY =
            "Token binding anomaly detected: type={}, user={}, tenant={}, expectedIp={}, actualIp={}, details={}";

    /**
     * Logs an anomaly event at WARN level.
     *
     * @param event the anomaly event to log
     */
    public void logAnomaly(AnomalyEvent event) {
        LOGGER.warn(LOG_TEMPLATE_ANOMALY,
                event.eventType(),
                event.username(),
                event.tenantId(),
                event.expectedIp(),
                event.actualIp(),
                event.details());
    }
}
