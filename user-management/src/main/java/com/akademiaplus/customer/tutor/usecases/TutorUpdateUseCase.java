/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.tutor.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.user.management.dto.TutorUpdateRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.TutorUpdateResponseDTO;
import org.modelmapper.ModelMapper;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles updating an existing tutor by mapping new field values
 * onto the persisted entity and its associated PII record.
 * <p>
 * Auth fields (provider, token) are updated directly on the
 * {@link CustomerAuthDataModel} if present, since customerAuth
 * is optional on tutors.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class TutorUpdateUseCase {

    public static final String MAP_NAME = "tutorUpdateMap";
    public static final String UPDATE_SUCCESS_MESSAGE = "Tutor updated successfully";

    private final TutorRepository tutorRepository;
    private final PersonPIIRepository personPIIRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;
    private final PiiNormalizer piiNormalizer;
    private final HashingService hashingService;

    /**
     * Updates the tutor identified by {@code tutorId}
     * within the current tenant context.
     *
     * @param tutorId the entity-specific tutor ID
     * @param dto     the updated field values
     * @return response containing the tutor ID and confirmation message
     * @throws EntityNotFoundException  if no tutor exists with the given composite key
     * @throws DuplicateEntityException if the updated email or phone belongs to another entity
     */
    @Transactional
    public TutorUpdateResponseDTO update(Long tutorId, TutorUpdateRequestDTO dto) {
        Long tenantId = tenantContextHolder.requireTenantId();

        TutorDataModel existing = tutorRepository
                .findById(new TutorDataModel.TutorCompositeId(tenantId, tutorId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.TUTOR, String.valueOf(tutorId)));

        modelMapper.map(dto, existing, MAP_NAME);

        PersonPIIDataModel pii = existing.getPersonPII();
        modelMapper.map(dto, pii);
        pii.setPhoneNumber(dto.getPhoneNumber());

        String normalizedEmail = piiNormalizer.normalizeEmail(pii.getEmail());
        String emailHash = hashingService.generateHash(normalizedEmail);
        pii.setEmailHash(emailHash);
        if (personPIIRepository.existsByEmailHashAndPersonPiiIdNot(emailHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.TUTOR, PiiField.EMAIL);
        }

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(pii.getPhoneNumber());
        String phoneHash = hashingService.generateHash(normalizedPhone);
        pii.setPhoneHash(phoneHash);
        if (personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(phoneHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.TUTOR, PiiField.PHONE_NUMBER);
        }

        CustomerAuthDataModel auth = existing.getCustomerAuth();
        if (auth != null) {
            if (dto.getProvider() != null && dto.getProvider().isPresent()) {
                auth.setProvider(dto.getProvider().get());
            }
            if (dto.getToken() != null && dto.getToken().isPresent()) {
                auth.setToken(dto.getToken().get());
            }
        }

        tutorRepository.saveAndFlush(existing);

        TutorUpdateResponseDTO response = new TutorUpdateResponseDTO();
        response.setTutorId(tutorId);
        response.setMessage(UPDATE_SUCCESS_MESSAGE);
        return response;
    }
}
