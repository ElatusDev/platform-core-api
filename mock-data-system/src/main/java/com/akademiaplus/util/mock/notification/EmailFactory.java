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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link EmailRequest} instances with fake data.
 */
@Component
@RequiredArgsConstructor
public class EmailFactory implements DataFactory<EmailFactory.EmailRequest> {

    private final EmailDataGenerator generator;

    @Override
    public List<EmailRequest> generate(int count) {
        List<EmailRequest> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(createRequest());
        }
        return items;
    }

    private EmailRequest createRequest() {
        return new EmailRequest(
                generator.subject(),
                generator.body(),
                generator.sender()
        );
    }

    /**
     * Lightweight request record used as the DTO type parameter.
     *
     * @param subject the email subject
     * @param body    the email body
     * @param sender  the sender address
     */
    public record EmailRequest(String subject, String body, String sender) { }
}
