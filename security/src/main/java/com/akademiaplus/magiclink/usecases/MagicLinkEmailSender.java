/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.usecases;

/**
 * Strategy interface for sending magic link emails.
 *
 * <p>Defined in the security module to decouple token generation from
 * email delivery infrastructure. Implementations live in the application
 * module where notification-system access is available.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
public interface MagicLinkEmailSender {

    /**
     * Sends a magic link email to the specified recipient.
     *
     * @param recipientEmail the email address to send to
     * @param subject        the email subject
     * @param htmlContent    the HTML body content
     */
    void send(String recipientEmail, String subject, String htmlContent);
}
