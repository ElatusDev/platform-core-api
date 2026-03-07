/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.attendance;

/**
 * Identifies the method used to verify student attendance.
 * Designed for extensibility as new verification mechanisms are added.
 */
public enum VerificationMethod {
    ANIMATED_QR,
    NFC,
    BLE,
    MANUAL
}
