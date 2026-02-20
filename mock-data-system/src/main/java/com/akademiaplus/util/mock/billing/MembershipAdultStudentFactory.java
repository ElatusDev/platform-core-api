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
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Factory for creating {@link MembershipAdultStudentCreationRequestDTO} instances with fake data.
 *
 * <p>Requires membership, course, and adult student IDs to be injected via setters
 * before {@link #generate(int)} is called.</p>
 */
@Component
public class MembershipAdultStudentFactory implements DataFactory<MembershipAdultStudentCreationRequestDTO> {

    private final Faker faker;

    @Setter
    private List<Long> availableMembershipIds = List.of();

    @Setter
    private List<Long> availableCourseIds = List.of();

    @Setter
    private List<Long> availableAdultStudentIds = List.of();

    public MembershipAdultStudentFactory() {
        this.faker = new Faker(new Locale("es", "MX"));
    }

    @Override
    public List<MembershipAdultStudentCreationRequestDTO> generate(int count) {
        if (availableMembershipIds.isEmpty()) {
            throw new IllegalStateException("availableMembershipIds must be set before generating");
        }
        if (availableCourseIds.isEmpty()) {
            throw new IllegalStateException("availableCourseIds must be set before generating");
        }
        if (availableAdultStudentIds.isEmpty()) {
            throw new IllegalStateException("availableAdultStudentIds must be set before generating");
        }
        List<MembershipAdultStudentCreationRequestDTO> associations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long membershipId = availableMembershipIds.get(i % availableMembershipIds.size());
            Long courseId = availableCourseIds.get(i % availableCourseIds.size());
            Long adultStudentId = availableAdultStudentIds.get(i % availableAdultStudentIds.size());
            associations.add(createAssociation(membershipId, courseId, adultStudentId));
        }
        return associations;
    }

    private MembershipAdultStudentCreationRequestDTO createAssociation(
            Long membershipId, Long courseId, Long adultStudentId) {
        LocalDate startDate = LocalDate.now().minusDays(faker.number().numberBetween(0, 90));
        MembershipAdultStudentCreationRequestDTO dto = new MembershipAdultStudentCreationRequestDTO();
        dto.setStartDate(startDate);
        dto.setDueDate(startDate.plusMonths(faker.number().numberBetween(1, 12)));
        dto.setMembershipId(membershipId);
        dto.setCourseId(courseId);
        dto.setAdultStudentId(adultStudentId);
        return dto;
    }
}
