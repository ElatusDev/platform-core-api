/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.usecases;

import com.akademiaplus.tokenbinding.usecases.domain.AnomalyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link AnomalyDetectionService}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("AnomalyDetectionService")
class AnomalyDetectionServiceTest {

    private static final String USERNAME = "testuser";
    private static final String EXPECTED_IP = "192.168.1.1";
    private static final String ACTUAL_IP = "10.0.0.1";
    private static final Long TENANT_ID = 1L;
    private static final long TIMESTAMP = 1700000000000L;

    private AnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        service = new AnomalyDetectionService();
    }

    @Test
    @DisplayName("should log full mismatch anomaly without throwing")
    void shouldLogAnomaly_whenFullMismatch() {
        // Given
        AnomalyEvent event = new AnomalyEvent(
                USERNAME, AnomalyEvent.EVENT_TYPE_FULL_MISMATCH,
                EXPECTED_IP, ACTUAL_IP, TENANT_ID, TIMESTAMP, null);

        // When / Then
        assertThatCode(() -> service.logAnomaly(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should log device mismatch anomaly without throwing")
    void shouldLogAnomaly_whenDeviceMismatch() {
        // Given
        AnomalyEvent event = new AnomalyEvent(
                USERNAME, AnomalyEvent.EVENT_TYPE_DEVICE_MISMATCH,
                EXPECTED_IP, ACTUAL_IP, TENANT_ID, TIMESTAMP, "extra details");

        // When / Then
        assertThatCode(() -> service.logAnomaly(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should log IP change anomaly without throwing")
    void shouldLogAnomaly_whenIpChange() {
        // Given
        AnomalyEvent event = new AnomalyEvent(
                USERNAME, AnomalyEvent.EVENT_TYPE_IP_CHANGE,
                null, ACTUAL_IP, TENANT_ID, TIMESTAMP, null);

        // When / Then
        assertThatCode(() -> service.logAnomaly(event)).doesNotThrowAnyException();
    }
}
