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
 * Factory for creating {@link EmailRecipientRequest} instances with fake data.
 *
 * <p>Requires email IDs to be injected via setter before
 * {@link #generate(int)} is called.</p>
 */
@Component
@RequiredArgsConstructor
public class EmailRecipientFactory
        implements DataFactory<EmailRecipientFactory.EmailRecipientRequest> {

    /** Error message when email IDs have not been set. */
    public static final String ERROR_EMAIL_IDS_NOT_SET =
            "availableEmailIds must be set before generating email recipients";

    private final EmailRecipientDataGenerator generator;

    @Setter
    private List<Long> availableEmailIds = List.of();

    @Override
    public List<EmailRecipientRequest> generate(int count) {
        if (availableEmailIds.isEmpty()) {
            throw new IllegalStateException(ERROR_EMAIL_IDS_NOT_SET);
        }
        List<EmailRecipientRequest> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long emailId = availableEmailIds.get(i % availableEmailIds.size());
            items.add(createRequest(emailId, i));
        }
        return items;
    }

    private EmailRecipientRequest createRequest(Long emailId, int index) {
        return new EmailRecipientRequest(
                emailId,
                generator.recipientEmail(index)
        );
    }

    /**
     * Lightweight request record used as the DTO type parameter.
     *
     * @param emailId        FK to the parent email
     * @param recipientEmail the recipient email address (part of composite PK)
     */
    public record EmailRecipientRequest(Long emailId, String recipientEmail) { }
}
