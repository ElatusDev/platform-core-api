/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases.domain;

import java.util.List;
import java.util.Map;

/**
 * Immutable configuration record for email delivery settings.
 * <p>
 * Encapsulates sender information, reply-to address, priority,
 * template data, attachment references, custom headers, and tracking preferences.
 *
 * @param fromEmail       sender email address
 * @param fromName        sender display name
 * @param replyTo         reply-to email address
 * @param priority        email priority level
 * @param templateId      optional template identifier for template-based emails
 * @param templateData    key-value pairs for template variable substitution
 * @param attachmentUrls  list of attachment URLs
 * @param customHeaders   custom email headers
 * @param trackingEnabled whether to enable open/click tracking
 */
public record EmailDeliveryConfig(
        String fromEmail,
        String fromName,
        String replyTo,
        String priority,
        Long templateId,
        Map<String, Object> templateData,
        List<String> attachmentUrls,
        Map<String, String> customHeaders,
        boolean trackingEnabled
) {
}
