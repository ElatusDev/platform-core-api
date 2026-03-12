/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import com.akademiaplus.util.base.DataFactory;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link CardPaymentInfoRequest} instances with fake data.
 *
 * <p>Requires payment–adult-student IDs to be injected via setter
 * before {@link #generate(int)} is called, so each card payment info
 * can reference a valid payment.</p>
 */
@Component
@RequiredArgsConstructor
public class CardPaymentInfoFactory implements DataFactory<CardPaymentInfoFactory.CardPaymentInfoRequest> {

    /** Error message when payment adult student IDs have not been set. */
    public static final String ERROR_PAYMENT_IDS_NOT_SET =
            "availablePaymentAdultStudentIds must be set before generating card payment infos";

    private final CardPaymentInfoDataGenerator generator;

    @Setter
    private List<Long> availablePaymentAdultStudentIds = List.of();

    @Override
    public List<CardPaymentInfoRequest> generate(int count) {
        if (availablePaymentAdultStudentIds.isEmpty()) {
            throw new IllegalStateException(ERROR_PAYMENT_IDS_NOT_SET);
        }
        List<CardPaymentInfoRequest> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long paymentId = availablePaymentAdultStudentIds
                    .get(i % availablePaymentAdultStudentIds.size());
            items.add(createRequest(paymentId));
        }
        return items;
    }

    private CardPaymentInfoRequest createRequest(Long paymentId) {
        return new CardPaymentInfoRequest(
                paymentId,
                generator.token(),
                generator.cardType()
        );
    }

    /**
     * Lightweight request record used as the DTO type parameter.
     *
     * @param paymentId the payment this card info belongs to
     * @param token     the tokenised card reference
     * @param cardType  the card network type
     */
    public record CardPaymentInfoRequest(Long paymentId, String token, String cardType) { }
}
