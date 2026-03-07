/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.currentuser.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.security.InternalAuthDataModel;
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
    private final HashingService hashingService;

    /**
     * Constructs the use case with all required dependencies.
     *
     * @param internalAuthRepository the internal auth repository
     * @param employeeRepository     the employee repository
     * @param collaboratorRepository the collaborator repository
     * @param hashingService         the hashing service for username lookup
     */
    public GetCurrentUserUseCase(InternalAuthRepository internalAuthRepository,
                                  EmployeeRepository employeeRepository,
                                  CollaboratorRepository collaboratorRepository,
                                  HashingService hashingService) {
        this.internalAuthRepository = internalAuthRepository;
        this.employeeRepository = employeeRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.hashingService = hashingService;
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
