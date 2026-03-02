/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles membership–adult-student association creation by transforming the
 * OpenAPI request DTO into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into nested JPA relationships (membership, course, adultStudent)
 * and the entity ID field.
 */
@Service
@RequiredArgsConstructor
public class MembershipAdultStudentCreationUseCase {
    public static final String MAP_NAME = "membershipAdultStudentMap";

    private final ApplicationContext applicationContext;
    private final MembershipAdultStudentRepository repository;
    private final MembershipRepository membershipRepository;
    private final CourseRepository courseRepository;
    private final AdultStudentRepository adultStudentRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    @Transactional
    public MembershipAdultStudentCreationResponseDTO create(MembershipAdultStudentCreationRequestDTO dto) {
        MembershipAdultStudentDataModel saved = repository.saveAndFlush(transform(dto));
        return modelMapper.map(saved, MembershipAdultStudentCreationResponseDTO.class);
    }

    public MembershipAdultStudentDataModel transform(MembershipAdultStudentCreationRequestDTO dto) {
        final MembershipAdultStudentDataModel model =
                applicationContext.getBean(MembershipAdultStudentDataModel.class);
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

        AdultStudentDataModel adultStudent = adultStudentRepository.findById(
                        new AdultStudentDataModel.AdultStudentCompositeId(tenantId, dto.getAdultStudentId()))
                .orElseThrow(() -> new IllegalArgumentException("Adult student not found: " + dto.getAdultStudentId()));
        model.setAdultStudent(adultStudent);

        return model;
    }
}
