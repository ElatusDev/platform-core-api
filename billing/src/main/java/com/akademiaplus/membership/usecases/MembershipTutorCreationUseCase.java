/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.users.customer.TutorDataModel;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles membership–tutor association creation by transforming the
 * OpenAPI request DTO into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into nested JPA relationships (membership, course, tutor)
 * and the entity ID field.
 */
@Service
@RequiredArgsConstructor
public class MembershipTutorCreationUseCase {
    public static final String MAP_NAME = "membershipTutorMap";

    private final ApplicationContext applicationContext;
    private final MembershipTutorRepository repository;
    private final MembershipRepository membershipRepository;
    private final CourseRepository courseRepository;
    private final TutorRepository tutorRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    @Transactional
    public MembershipTutorCreationResponseDTO create(MembershipTutorCreationRequestDTO dto) {
        MembershipTutorDataModel saved = repository.save(transform(dto));
        return modelMapper.map(saved, MembershipTutorCreationResponseDTO.class);
    }

    public MembershipTutorDataModel transform(MembershipTutorCreationRequestDTO dto) {
        final MembershipTutorDataModel model =
                applicationContext.getBean(MembershipTutorDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);

        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException("Tenant context is required"));

        MembershipDataModel membership = membershipRepository.findById(
                        new MembershipDataModel.MembershipCompositeId(tenantId, dto.getMembershipId()))
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + dto.getMembershipId()));
        model.setMembership(membership);

        CourseDataModel course = courseRepository.findById(
                        new CourseDataModel.CourseCompositeId(tenantId, dto.getCourseId()))
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + dto.getCourseId()));
        model.setCourse(course);

        TutorDataModel tutor = tutorRepository.findById(
                        new TutorDataModel.TutorCompositeId(tenantId, dto.getTutorId()))
                .orElseThrow(() -> new IllegalArgumentException("Tutor not found: " + dto.getTutorId()));
        model.setTutor(tutor);

        return model;
    }
}
