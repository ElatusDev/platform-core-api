/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.usecases;

import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Use case responsible for creating collaborators.
 * <p>
 * Uses a named TypeMap ({@value TYPE_MAP}) and prototype-scoped beans via
 * {@link ApplicationContext} to align with the Employee creation pattern.
 */
@Service
public class CollaboratorCreationUseCase {
    public static final String TYPE_MAP = "collaboratorMap";

    private final ApplicationContext applicationContext;
    private final CollaboratorRepository repository;
    private final PersonPIIRepository personPIIRepository;
    private final ModelMapper modelMapper;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;

    public CollaboratorCreationUseCase(ApplicationContext applicationContext,
                                       CollaboratorRepository repository,
                                       PersonPIIRepository personPIIRepository,
                                       ModelMapper modelMapper,
                                       HashingService hashingService,
                                       PiiNormalizer piiNormalizer) {
        this.applicationContext = applicationContext;
        this.repository = repository;
        this.personPIIRepository = personPIIRepository;
        this.modelMapper = modelMapper;
        this.hashingService = hashingService;
        this.piiNormalizer = piiNormalizer;
    }

    @Transactional
    public CollaboratorCreationResponseDTO create(CollaboratorCreationRequestDTO dto) {
        CollaboratorDataModel model = transform(dto);
        PersonPIIDataModel pii = model.getPersonPII();
        if (personPIIRepository.existsByEmailHash(pii.getEmailHash())) {
            throw new DuplicateEntityException(EntityType.COLLABORATOR, PiiField.EMAIL);
        }
        if (personPIIRepository.existsByPhoneHash(pii.getPhoneHash())) {
            throw new DuplicateEntityException(EntityType.COLLABORATOR, PiiField.PHONE_NUMBER);
        }
        return modelMapper.map(repository.saveAndFlush(model), CollaboratorCreationResponseDTO.class);
    }

    public CollaboratorDataModel transform(CollaboratorCreationRequestDTO dto) {
        final InternalAuthDataModel internalAuthDataModel = applicationContext.getBean(InternalAuthDataModel.class);
        modelMapper.map(dto, internalAuthDataModel);

        final PersonPIIDataModel personPIIDataModel = applicationContext.getBean(PersonPIIDataModel.class);
        modelMapper.map(dto, personPIIDataModel);
        personPIIDataModel.setPhoneNumber(dto.getPhoneNumber());

        final CollaboratorDataModel model = applicationContext.getBean(CollaboratorDataModel.class);
        modelMapper.map(dto, model, TYPE_MAP);

        model.setPersonPII(personPIIDataModel);
        model.setInternalAuth(internalAuthDataModel);

        String normalizedEmail = piiNormalizer.normalizeEmail(model.getPersonPII().getEmail());
        model.getPersonPII().setEmailHash(hashingService.generateHash(normalizedEmail));

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(model.getPersonPII().getPhoneNumber());
        model.getPersonPII().setPhoneHash(hashingService.generateHash(normalizedPhone));

        internalAuthDataModel.setUsernameHash(hashingService.generateHash(internalAuthDataModel.getUsername()));

        return model;
    }
}
