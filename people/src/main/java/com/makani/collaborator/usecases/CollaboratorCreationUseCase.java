/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.makani.collaborator.usecases;

import com.makani.PersonPIIDataModel;
import com.makani.people.collaborator.CollaboratorDataModel;
import com.makani.collaborator.interfaceadapters.CollaboratorRepository;
import com.makani.security.user.InternalAuthDataModel;
import com.makani.utilities.security.HashingService;
import com.makani.utilities.security.PiiNormalizer;
import openapi.makani.domain.people.dto.CollaboratorCreationRequestDTO;
import openapi.makani.domain.people.dto.CollaboratorCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class CollaboratorCreationUseCase {
    private final CollaboratorRepository repository;
    private final ModelMapper modelMapper;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;
    private final Set<InternalAuthDataModel> set;
    public static final String TYPE_MAP = "collaboratorMap";

    public CollaboratorCreationUseCase(CollaboratorRepository repository,
                                       ModelMapper modelMapper,
                                       HashingService hashingService,
                                       PiiNormalizer piiNormalizer) {
        this.repository = repository;
        this.modelMapper = modelMapper;
        this.hashingService = hashingService;
        this.piiNormalizer = piiNormalizer;
        this.set = new HashSet<>();
    }

    @Transactional
    public CollaboratorCreationResponseDTO create(CollaboratorCreationRequestDTO dto) {
        return modelMapper.map(repository.save(transform(dto)), CollaboratorCreationResponseDTO.class);
    }

    public CollaboratorDataModel transform(CollaboratorCreationRequestDTO dto) {
        final InternalAuthDataModel internalAuthDataModel= modelMapper.map(dto.getInternalAuth(), InternalAuthDataModel.class);
        final PersonPIIDataModel personPIIDataModel = modelMapper.map(dto, PersonPIIDataModel.class);
        CollaboratorDataModel model =  modelMapper.map(dto, CollaboratorDataModel.class, TYPE_MAP);

        model.setPersonPII(personPIIDataModel);
        model.setInternalAuth(internalAuthDataModel);

        String normalizedEmail = piiNormalizer.normalizeEmail(model.getPersonPII().getEmail());
        model.getPersonPII().setEmailHash(hashingService.generateHash(normalizedEmail));

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(model.getPersonPII().getPhone());
        model.getPersonPII().setPhoneHash(hashingService.generateHash(normalizedPhone));

        internalAuthDataModel.setUsernameHash(hashingService.generateHash(internalAuthDataModel.getUsername()));

        return model;
    }
}
