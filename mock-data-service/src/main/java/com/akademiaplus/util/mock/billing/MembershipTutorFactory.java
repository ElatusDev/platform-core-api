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
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Factory for creating {@link MembershipTutorCreationRequestDTO} instances with fake data.
 *
 * <p>Requires membership, course, and tutor IDs to be injected via setters
 * before {@link #generate(int)} is called.</p>
 */
@Component
public class MembershipTutorFactory implements DataFactory<MembershipTutorCreationRequestDTO> {

    private final Faker faker;

    @Setter
    private List<Long> availableMembershipIds = List.of();

    @Setter
    private List<Long> availableCourseIds = List.of();

    @Setter
    private List<Long> availableTutorIds = List.of();

    public MembershipTutorFactory() {
        this.faker = new Faker(Locale.of("es", "MX"));
    }

    @Override
    public List<MembershipTutorCreationRequestDTO> generate(int count) {
        if (availableMembershipIds.isEmpty()) {
            throw new IllegalStateException("availableMembershipIds must be set before generating");
        }
        if (availableCourseIds.isEmpty()) {
            throw new IllegalStateException("availableCourseIds must be set before generating");
        }
        if (availableTutorIds.isEmpty()) {
            throw new IllegalStateException("availableTutorIds must be set before generating");
        }
        List<MembershipTutorCreationRequestDTO> associations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long membershipId = availableMembershipIds.get(i % availableMembershipIds.size());
            Long courseId = availableCourseIds.get(i % availableCourseIds.size());
            Long tutorId = availableTutorIds.get(i % availableTutorIds.size());
            associations.add(createAssociation(membershipId, courseId, tutorId));
        }
        return associations;
    }

    private MembershipTutorCreationRequestDTO createAssociation(
            Long membershipId, Long courseId, Long tutorId) {
        LocalDate startDate = LocalDate.now().minusDays(faker.number().numberBetween(0, 90));
        MembershipTutorCreationRequestDTO dto = new MembershipTutorCreationRequestDTO();
        dto.setStartDate(startDate);
        dto.setDueDate(startDate.plusMonths(faker.number().numberBetween(1, 12)));
        dto.setMembershipId(membershipId);
        dto.setCourseId(courseId);
        dto.setTutorId(tutorId);
        return dto;
    }
}
