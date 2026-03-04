/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notifications.DeliveryStatus;

/**
 * Immutable result of a notification delivery attempt.
 *
 * @param status        the delivery outcome (SENT or FAILED)
 * @param failureReason the reason for failure, or {@code null} if successful
 */
public record DeliveryResult(DeliveryStatus status, String failureReason) {

    /**
     * Creates a successful delivery result.
     *
     * @return result with {@link DeliveryStatus#SENT} and no failure reason
     */
    public static DeliveryResult sent() {
        return new DeliveryResult(DeliveryStatus.SENT, null);
    }

    /**
     * Creates a failed delivery result.
     *
     * @param reason the failure reason
     * @return result with {@link DeliveryStatus#FAILED} and the given reason
     */
    public static DeliveryResult failed(String reason) {
        return new DeliveryResult(DeliveryStatus.FAILED, reason);
    }
}
