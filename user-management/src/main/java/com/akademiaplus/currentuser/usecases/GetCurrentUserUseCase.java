/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.currentuser.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.AbstractUser;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import openapi.akademiaplus.domain.user.management.dto.GetCurrentUserResponseDTO;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resolves the currently authenticated user's profile from the JWT token.
 *
 * <p>Extracts the username from the Spring Security context, looks up the
 * internal authentication record, then queries Employee and Collaborator
 * repositories to find the matching profile.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetCurrentUserUseCase {

    /** Error message when no authentication is present in the security context. */
    public static final String ERROR_NO_AUTHENTICATION = "No authentication present in security context";

    /** User type constant for employee profiles. */
    public static final String USER_TYPE_EMPLOYEE = "EMPLOYEE";

    /** User type constant for collaborator profiles. */
    public static final String USER_TYPE_COLLABORATOR = "COLLABORATOR";

    private final InternalAuthRepository internalAuthRepository;
    private final EmployeeRepository employeeRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final AdultStudentRepository adultStudentRepository;
    private final TutorRepository tutorRepository;
    private final HashingService hashingService;
    private final UserContextHolder userContextHolder;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Constructs the use case with all required dependencies.
     *
     * @param internalAuthRepository the internal auth repository
     * @param employeeRepository     the employee repository
     * @param collaboratorRepository the collaborator repository
     * @param adultStudentRepository the adult student repository
     * @param tutorRepository        the tutor repository
     * @param hashingService         the hashing service for username lookup
     * @param userContextHolder      the user context holder for customer profiles
     * @param tenantContextHolder    the tenant context holder
     */
    public GetCurrentUserUseCase(InternalAuthRepository internalAuthRepository,
                                  EmployeeRepository employeeRepository,
                                  CollaboratorRepository collaboratorRepository,
                                  AdultStudentRepository adultStudentRepository,
                                  TutorRepository tutorRepository,
                                  HashingService hashingService,
                                  UserContextHolder userContextHolder,
                                  TenantContextHolder tenantContextHolder) {
        this.internalAuthRepository = internalAuthRepository;
        this.employeeRepository = employeeRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.adultStudentRepository = adultStudentRepository;
        this.tutorRepository = tutorRepository;
        this.hashingService = hashingService;
        this.userContextHolder = userContextHolder;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Resolves the current user's profile from the security context.
     *
     * @return the current user's profile DTO
     * @throws IllegalStateException   if no authentication is present
     * @throws EntityNotFoundException if no user profile is found for the authenticated user
     */
    public GetCurrentUserResponseDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException(ERROR_NO_AUTHENTICATION);
        }

        java.util.Optional<UserContextHolder.UserContext> userContext = userContextHolder.get();
        if (userContext.isPresent()) {
            return resolveCustomerProfile(userContext.get());
        }

        String username = authentication.getName();
        String usernameHash = hashingService.generateHash(username);

        InternalAuthDataModel auth = internalAuthRepository.findByUsernameHash(usernameHash)
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.INTERNAL_AUTH, username));

        Long internalAuthId = auth.getInternalAuthId();

        return employeeRepository.findByInternalAuthId(internalAuthId)
                .map(employee -> mapEmployee(employee, auth))
                .orElseGet(() -> collaboratorRepository.findByInternalAuthId(internalAuthId)
                        .map(collaborator -> mapCollaborator(collaborator, auth))
                        .orElseThrow(() -> new EntityNotFoundException(
                                EntityType.USER, username)));
    }

    private GetCurrentUserResponseDTO resolveCustomerProfile(UserContextHolder.UserContext userContext) {
        String profileType = userContext.profileType();
        Long profileId = userContext.profileId();
        Long tenantId = tenantContextHolder.requireTenantId();

        AbstractUser user;
        GetCurrentUserResponseDTO.UserTypeEnum userTypeEnum;

        if (JwtTokenProvider.PROFILE_TYPE_ADULT_STUDENT.equals(profileType)) {
            user = adultStudentRepository.findById(
                    new com.akademiaplus.users.customer.AdultStudentDataModel.AdultStudentCompositeId(tenantId, profileId))
                    .orElseThrow(() -> new EntityNotFoundException(EntityType.ADULT_STUDENT, profileId.toString()));
            userTypeEnum = GetCurrentUserResponseDTO.UserTypeEnum.ADULT_STUDENT;
        } else if (JwtTokenProvider.PROFILE_TYPE_TUTOR.equals(profileType)) {
            user = tutorRepository.findById(
                    new com.akademiaplus.users.customer.TutorDataModel.TutorCompositeId(tenantId, profileId))
                    .orElseThrow(() -> new EntityNotFoundException(EntityType.TUTOR, profileId.toString()));
            userTypeEnum = GetCurrentUserResponseDTO.UserTypeEnum.TUTOR;
        } else {
            throw new EntityNotFoundException(profileType, profileId.toString());
        }

        GetCurrentUserResponseDTO dto = new GetCurrentUserResponseDTO();
        dto.setUserType(userTypeEnum);
        mapPii(dto, user);
        dto.setBirthdate(user.getBirthDate());
        dto.setEntryDate(user.getEntryDate());
        return dto;
    }

    private GetCurrentUserResponseDTO mapEmployee(EmployeeDataModel employee,
                                                   InternalAuthDataModel auth) {
        GetCurrentUserResponseDTO dto = new GetCurrentUserResponseDTO();
        dto.setUserType(GetCurrentUserResponseDTO.UserTypeEnum.EMPLOYEE);
        dto.setEmployeeId(JsonNullable.of(employee.getEmployeeId()));
        dto.setInternalAuthId(auth.getInternalAuthId());
        dto.setUsername(auth.getUsername());
        dto.setRole(auth.getRole());
        dto.setEmployeeType(JsonNullable.of(employee.getEmployeeType()));
        dto.setBirthdate(employee.getBirthDate());
        dto.setEntryDate(employee.getEntryDate());
        mapPii(dto, employee);
        return dto;
    }

    private GetCurrentUserResponseDTO mapCollaborator(CollaboratorDataModel collaborator,
                                                       InternalAuthDataModel auth) {
        GetCurrentUserResponseDTO dto = new GetCurrentUserResponseDTO();
        dto.setUserType(GetCurrentUserResponseDTO.UserTypeEnum.COLLABORATOR);
        dto.setCollaboratorId(JsonNullable.of(collaborator.getCollaboratorId()));
        dto.setInternalAuthId(auth.getInternalAuthId());
        dto.setUsername(auth.getUsername());
        dto.setRole(auth.getRole());
        dto.setSkills(JsonNullable.of(collaborator.getSkills()));
        dto.setBirthdate(collaborator.getBirthDate());
        dto.setEntryDate(collaborator.getEntryDate());
        mapPii(dto, collaborator);
        return dto;
    }

    private void mapPii(GetCurrentUserResponseDTO dto,
                         com.akademiaplus.users.base.AbstractUser user) {
        if (user.getPersonPII() != null) {
            dto.setFirstName(user.getPersonPII().getFirstName());
            dto.setLastName(user.getPersonPII().getLastName());
            dto.setEmail(user.getPersonPII().getEmail());
            dto.setPhoneNumber(user.getPersonPII().getPhoneNumber());
            dto.setAddress(user.getPersonPII().getAddress());
            dto.setZipCode(user.getPersonPII().getZipCode());
        }
    }
}
