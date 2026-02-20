/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.usecases;

import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import openapi.akademiaplus.domain.user.management.dto.GetCollaboratorResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetCollaboratorByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final CollaboratorRepository repository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    public GetCollaboratorByIdUseCase(CollaboratorRepository repository,
                                       TenantContextHolder tenantContextHolder,
                                       ModelMapper modelMapper) {
        this.repository = repository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    public GetCollaboratorResponseDTO get(Long collaboratorId) {
          Long tenantId = tenantContextHolder.getTenantId()
                  .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
          Optional<CollaboratorDataModel> queryResult = repository.findById(
                  new CollaboratorDataModel.CollaboratorCompositeId(tenantId, collaboratorId));
          if(queryResult.isPresent()) {
              CollaboratorDataModel found = queryResult.get();
              GetCollaboratorResponseDTO dto = modelMapper.map(found, GetCollaboratorResponseDTO.class);
              modelMapper.map(found.getPersonPII(), dto);
              return dto;
          } else {
              throw new EntityNotFoundException(EntityType.COLLABORATOR, String.valueOf(collaboratorId));
          }
    }
}
