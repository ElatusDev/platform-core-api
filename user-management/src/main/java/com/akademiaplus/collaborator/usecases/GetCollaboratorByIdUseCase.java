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
import com.akademiaplus.exception.CollaboratorNotFoundException;
import openapi.akademiaplus.domain.user.management.dto.GetCollaboratorResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetCollaboratorByIdUseCase {
    private final CollaboratorRepository repository;
    private final ModelMapper modelMapper;

    public GetCollaboratorByIdUseCase(CollaboratorRepository repository, ModelMapper modelMapper) {
        this.repository = repository;
        this.modelMapper = modelMapper;
    }

    public GetCollaboratorResponseDTO get(Integer collaboratorId) {
          Optional<CollaboratorDataModel> queryResult = repository.findById(collaboratorId);
          if(queryResult.isPresent()) {
              CollaboratorDataModel found = queryResult.get();
              GetCollaboratorResponseDTO dto = modelMapper.map(found, GetCollaboratorResponseDTO.class);
              modelMapper.map(found.getPersonPII(), dto);
              return dto;
          } else {
              throw new CollaboratorNotFoundException(String.valueOf(collaboratorId));
          }
    }
}
