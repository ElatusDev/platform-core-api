/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.employee.usecases;

import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.user.management.dto.EmployeeUpdateRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.EmployeeUpdateResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles updating an existing employee by mapping new field values
 * onto the persisted entity and its associated PII and auth records.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class EmployeeUpdateUseCase {

    public static final String MAP_NAME = "employeeUpdateMap";
    public static final String UPDATE_SUCCESS_MESSAGE = "Employee updated successfully";

    private final EmployeeRepository employeeRepository;
    private final PersonPIIRepository personPIIRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;
    private final PiiNormalizer piiNormalizer;
    private final HashingService hashingService;

    /**
     * Updates the employee identified by {@code employeeId}
     * within the current tenant context.
     *
     * @param employeeId the entity-specific employee ID
     * @param dto        the updated field values
     * @return response containing the employee ID and confirmation message
     * @throws EntityNotFoundException  if no employee exists with the given composite key
     * @throws DuplicateEntityException if the updated email or phone belongs to another entity
     */
    @Transactional
    public EmployeeUpdateResponseDTO update(Long employeeId, EmployeeUpdateRequestDTO dto) {
        Long tenantId = tenantContextHolder.requireTenantId();

        EmployeeDataModel existing = employeeRepository
                .findById(new EmployeeDataModel.EmployeeCompositeId(tenantId, employeeId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.EMPLOYEE, String.valueOf(employeeId)));

        modelMapper.map(dto, existing, MAP_NAME);

        PersonPIIDataModel pii = existing.getPersonPII();
        modelMapper.map(dto, pii);
        pii.setPhoneNumber(dto.getPhoneNumber());

        String normalizedEmail = piiNormalizer.normalizeEmail(pii.getEmail());
        String emailHash = hashingService.generateHash(normalizedEmail);
        pii.setEmailHash(emailHash);
        if (personPIIRepository.existsByEmailHashAndPersonPiiIdNot(emailHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.EMPLOYEE, PiiField.EMAIL);
        }

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(pii.getPhoneNumber());
        String phoneHash = hashingService.generateHash(normalizedPhone);
        pii.setPhoneHash(phoneHash);
        if (personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(phoneHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.EMPLOYEE, PiiField.PHONE_NUMBER);
        }

        existing.getInternalAuth().setRole(dto.getRole());

        employeeRepository.saveAndFlush(existing);

        EmployeeUpdateResponseDTO response = new EmployeeUpdateResponseDTO();
        response.setEmployeeId(employeeId);
        response.setMessage(UPDATE_SUCCESS_MESSAGE);
        return response;
    }
}
