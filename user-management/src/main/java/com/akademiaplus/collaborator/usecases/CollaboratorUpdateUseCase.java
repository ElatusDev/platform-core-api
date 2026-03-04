/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorUpdateRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorUpdateResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles updating an existing collaborator by mapping new field values
 * onto the persisted entity and its associated PII and auth records.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class CollaboratorUpdateUseCase {

    public static final String MAP_NAME = "collaboratorUpdateMap";
    public static final String UPDATE_SUCCESS_MESSAGE = "Collaborator updated successfully";

    private final CollaboratorRepository collaboratorRepository;
    private final PersonPIIRepository personPIIRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;
    private final PiiNormalizer piiNormalizer;
    private final HashingService hashingService;

    /**
     * Updates the collaborator identified by {@code collaboratorId}
     * within the current tenant context.
     *
     * @param collaboratorId the entity-specific collaborator ID
     * @param dto            the updated field values
     * @return response containing the collaborator ID and confirmation message
     * @throws EntityNotFoundException  if no collaborator exists with the given composite key
     * @throws DuplicateEntityException if the updated email or phone belongs to another entity
     */
    @Transactional
    public CollaboratorUpdateResponseDTO update(Long collaboratorId,
                                                 CollaboratorUpdateRequestDTO dto) {
        Long tenantId = tenantContextHolder.requireTenantId();

        CollaboratorDataModel existing = collaboratorRepository
                .findById(new CollaboratorDataModel.CollaboratorCompositeId(tenantId, collaboratorId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.COLLABORATOR, String.valueOf(collaboratorId)));

        modelMapper.map(dto, existing, MAP_NAME);

        PersonPIIDataModel pii = existing.getPersonPII();
        modelMapper.map(dto, pii);
        pii.setPhoneNumber(dto.getPhoneNumber());

        String normalizedEmail = piiNormalizer.normalizeEmail(pii.getEmail());
        String emailHash = hashingService.generateHash(normalizedEmail);
        pii.setEmailHash(emailHash);
        if (personPIIRepository.existsByEmailHashAndPersonPiiIdNot(emailHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.COLLABORATOR, PiiField.EMAIL);
        }

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(pii.getPhoneNumber());
        String phoneHash = hashingService.generateHash(normalizedPhone);
        pii.setPhoneHash(phoneHash);
        if (personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(phoneHash, pii.getPersonPiiId())) {
            throw new DuplicateEntityException(EntityType.COLLABORATOR, PiiField.PHONE_NUMBER);
        }

        existing.getInternalAuth().setRole(dto.getRole());

        collaboratorRepository.saveAndFlush(existing);

        CollaboratorUpdateResponseDTO response = new CollaboratorUpdateResponseDTO();
        response.setCollaboratorId(collaboratorId);
        response.setMessage(UPDATE_SUCCESS_MESSAGE);
        return response;
    }
}
