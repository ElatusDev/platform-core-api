/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import com.akademiaplus.util.base.DataFactory;
import lombok.Setter;
import net.datafaker.Faker;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Factory for creating {@link PaymentTutorCreationRequestDTO} instances with fake data.
 *
 * <p>Requires membership–tutor association IDs to be injected via setter
 * before {@link #generate(int)} is called.</p>
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class PaymentTutorFactory implements DataFactory<PaymentTutorCreationRequestDTO> {

    private static final List<String> PAYMENT_METHODS = Arrays.asList(
            "cash", "card", "transfer", "check"
    );

    private final Faker faker;
    private final Random random;

    @Setter
    private List<Long> availableMembershipTutorIds = List.of();

    public PaymentTutorFactory() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    @Override
    public List<PaymentTutorCreationRequestDTO> generate(int count) {
        if (availableMembershipTutorIds.isEmpty()) {
            throw new IllegalStateException(
                    "availableMembershipTutorIds must be set before generating payments");
        }
        List<PaymentTutorCreationRequestDTO> payments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long membershipTutorId = availableMembershipTutorIds
                    .get(i % availableMembershipTutorIds.size());
            payments.add(createPayment(membershipTutorId));
        }
        return payments;
    }

    private PaymentTutorCreationRequestDTO createPayment(Long membershipTutorId) {
        PaymentTutorCreationRequestDTO dto = new PaymentTutorCreationRequestDTO();
        dto.setPaymentDate(LocalDate.now().minusDays(faker.number().numberBetween(0, 60)));
        dto.setAmount((double) faker.number().numberBetween(500, 5000));
        dto.setPaymentMethod(PAYMENT_METHODS.get(random.nextInt(PAYMENT_METHODS.size())));
        dto.setMembershipTutorId(membershipTutorId);
        return dto;
    }
}
