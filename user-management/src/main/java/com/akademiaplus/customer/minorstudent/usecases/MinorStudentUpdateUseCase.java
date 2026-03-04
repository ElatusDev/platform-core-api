/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentUpdateRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentUpdateResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles updating an existing minor student by mapping new field values
 * onto the persisted entity and its associated PII record.
 * <p>
 * Validates that the referenced tutor exists before saving.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class MinorStudentUpdateUseCase {

    public static final String MAP_NAME = "minorStudentUpdateMap";
    public static final String UPDATE_SUCCESS_MESSAGE = "Minor student updated successfully";

    private final MinorStudentRepository minorStudentRepository;
    private final TutorRepository tutorRepository;
    private final PersonPIIRepository personPIIRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;
    private final PiiNormalizer piiNormalizer;
    private final HashingService hashingService;

    /**
     * Updates the minor student identified by {@code minorStudentId}
     * within the current tenant context.
     *
     * @param minorStudentId the entity-specific minor student ID
     * @param dto            the updated field values
     * @return response containing the minor student ID and confirmation message
     * @throws EntityNotFoundException  if no minor student or referenced tutor exists
     * @throws DuplicateEntityException if the updated email or phone belongs to another entity
     */
    @Transactional
    public MinorStudentUpdateResponseDTO update(Long minorStudentId,
                                                 MinorStudentUpdateRequestDTO dto) {
        Long tenantId = tenantContextHolder.requireTenantId();

        MinorStudentDataModel existing = minorStudentRepository
                .findById(new MinorStudentDataModel.MinorStudentCompositeId(tenantId, minorStudentId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.MINOR_STUDENT, String.valueOf(minorStudentId)));

        modelMapper.map(dto, existing, MAP_NAME);

        tutorRepository.findById(new TutorDataModel.TutorCompositeId(tenantId, dto.getTutorId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.TUTOR, String.valueOf(dto.getTutorId())));

        PersonPIIDataModel pii = existing.getPersonPII();
        modelMapper.map(dto, pii);
        pii.setPhoneNumber(dto.getPhoneNumber());

        String normalizedEmail = piiNormalizer.normalizeEmail(pii.getEmail());
        String emailHash = hashingService.generateHash(normalizedEmail);
        pii.setEmailHash(emailHash);
        if (personPIIRepository.existsByEmailHashAndPersonPiiIdNot(emailHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.MINOR_STUDENT, PiiField.EMAIL);
        }

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(pii.getPhoneNumber());
        String phoneHash = hashingService.generateHash(normalizedPhone);
        pii.setPhoneHash(phoneHash);
        if (personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(phoneHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.MINOR_STUDENT, PiiField.PHONE_NUMBER);
        }

        minorStudentRepository.saveAndFlush(existing);

        MinorStudentUpdateResponseDTO response = new MinorStudentUpdateResponseDTO();
        response.setMinorStudentId(minorStudentId);
        response.setMessage(UPDATE_SUCCESS_MESSAGE);
        return response;
    }
}
