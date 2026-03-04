/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentUpdateRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentUpdateResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles updating an existing adult student by mapping new field values
 * onto the persisted entity and its associated PII record.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) that skips nested object setters
 * to prevent ModelMapper from deep-matching into JPA relationships.
 * PII fields are mapped separately and rehashed for duplicate validation
 * with self-exclusion.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class AdultStudentUpdateUseCase {

    /**
     * Named TypeMap identifier for entity-level update mapping.
     */
    public static final String MAP_NAME = "adultStudentUpdateMap";

    public static final String UPDATE_SUCCESS_MESSAGE = "Adult student updated successfully";

    private final AdultStudentRepository adultStudentRepository;
    private final PersonPIIRepository personPIIRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;
    private final PiiNormalizer piiNormalizer;
    private final HashingService hashingService;

    /**
     * Updates the adult student identified by {@code adultStudentId}
     * within the current tenant context.
     *
     * @param adultStudentId the entity-specific adult student ID
     * @param dto            the updated field values
     * @return response containing the adult student ID and confirmation message
     * @throws EntityNotFoundException  if no adult student exists with the given composite key
     * @throws DuplicateEntityException if the updated email or phone belongs to another entity
     */
    @Transactional
    public AdultStudentUpdateResponseDTO update(Long adultStudentId,
                                                 AdultStudentUpdateRequestDTO dto) {
        Long tenantId = tenantContextHolder.requireTenantId();

        AdultStudentDataModel existing = adultStudentRepository
                .findById(new AdultStudentDataModel.AdultStudentCompositeId(tenantId, adultStudentId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.ADULT_STUDENT, String.valueOf(adultStudentId)));

        modelMapper.map(dto, existing, MAP_NAME);

        PersonPIIDataModel pii = existing.getPersonPII();
        modelMapper.map(dto, pii);
        pii.setPhoneNumber(dto.getPhoneNumber());

        String normalizedEmail = piiNormalizer.normalizeEmail(pii.getEmail());
        String emailHash = hashingService.generateHash(normalizedEmail);
        pii.setEmailHash(emailHash);
        if (personPIIRepository.existsByEmailHashAndPersonPiiIdNot(emailHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.ADULT_STUDENT, PiiField.EMAIL);
        }

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(pii.getPhoneNumber());
        String phoneHash = hashingService.generateHash(normalizedPhone);
        pii.setPhoneHash(phoneHash);
        if (personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(phoneHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.ADULT_STUDENT, PiiField.PHONE_NUMBER);
        }

        adultStudentRepository.saveAndFlush(existing);

        AdultStudentUpdateResponseDTO response = new AdultStudentUpdateResponseDTO();
        response.setAdultStudentId(adultStudentId);
        response.setMessage(UPDATE_SUCCESS_MESSAGE);
        return response;
    }
}
