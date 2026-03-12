/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.util.base.DataFactory;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link EmailAttachmentRequest} instances with fake data.
 *
 * <p>Requires email IDs to be injected via setter before
 * {@link #generate(int)} is called.</p>
 */
@Component
@RequiredArgsConstructor
public class EmailAttachmentFactory
        implements DataFactory<EmailAttachmentFactory.EmailAttachmentRequest> {

    /** Error message when email IDs have not been set. */
    public static final String ERROR_EMAIL_IDS_NOT_SET =
            "availableEmailIds must be set before generating email attachments";

    private final EmailAttachmentDataGenerator generator;

    @Setter
    private List<Long> availableEmailIds = List.of();

    @Override
    public List<EmailAttachmentRequest> generate(int count) {
        if (availableEmailIds.isEmpty()) {
            throw new IllegalStateException(ERROR_EMAIL_IDS_NOT_SET);
        }
        List<EmailAttachmentRequest> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long emailId = availableEmailIds.get(i % availableEmailIds.size());
            items.add(createRequest(emailId, i));
        }
        return items;
    }

    private EmailAttachmentRequest createRequest(Long emailId, int index) {
        return new EmailAttachmentRequest(
                emailId,
                generator.attachmentUrl(index)
        );
    }

    /**
     * Lightweight request record used as the DTO type parameter.
     *
     * @param emailId       FK to the parent email
     * @param attachmentUrl the attachment URL (part of composite PK)
     */
    public record EmailAttachmentRequest(Long emailId, String attachmentUrl) { }
}
