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
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Factory for creating {@link PaymentAdultStudentCreationRequestDTO} instances with fake data.
 *
 * <p>Requires membership–adult-student association IDs to be injected via setter
 * before {@link #generate(int)} is called.</p>
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class PaymentAdultStudentFactory implements DataFactory<PaymentAdultStudentCreationRequestDTO> {

    static final String ERROR_MEMBERSHIP_ADULT_STUDENT_IDS_NOT_SET =
            "availableMembershipAdultStudentIds must be set before generating payments";

    private static final List<String> PAYMENT_METHODS = Arrays.asList(
            "cash", "card", "transfer", "check"
    );

    private final Faker faker;
    private final Random random;

    @Setter
    private List<Long> availableMembershipAdultStudentIds = List.of();

    public PaymentAdultStudentFactory() {
        this.faker = new Faker(Locale.of("es", "MX"));
        this.random = new Random();
    }

    @Override
    public List<PaymentAdultStudentCreationRequestDTO> generate(int count) {
        if (availableMembershipAdultStudentIds.isEmpty()) {
            throw new IllegalStateException(ERROR_MEMBERSHIP_ADULT_STUDENT_IDS_NOT_SET);
        }
        List<PaymentAdultStudentCreationRequestDTO> payments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long membershipAdultStudentId = availableMembershipAdultStudentIds
                    .get(i % availableMembershipAdultStudentIds.size());
            payments.add(createPayment(membershipAdultStudentId));
        }
        return payments;
    }

    private PaymentAdultStudentCreationRequestDTO createPayment(Long membershipAdultStudentId) {
        PaymentAdultStudentCreationRequestDTO dto = new PaymentAdultStudentCreationRequestDTO();
        dto.setPaymentDate(LocalDate.now().minusDays(faker.number().numberBetween(0, 60)));
        dto.setAmount((double) faker.number().numberBetween(500, 5000));
        dto.setPaymentMethod(PAYMENT_METHODS.get(random.nextInt(PAYMENT_METHODS.size())));
        dto.setMembershipAdultStudentId(membershipAdultStudentId);
        return dto;
    }
}
