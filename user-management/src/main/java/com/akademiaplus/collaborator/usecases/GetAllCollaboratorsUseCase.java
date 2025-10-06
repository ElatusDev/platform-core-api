/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import openapi.akademiaplus.domain.user.management.dto.GetCollaboratorResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetAllCollaboratorsUseCase {

    private final CollaboratorRepository repository;
    private final ModelMapper modelMapper;

    public GetAllCollaboratorsUseCase(CollaboratorRepository repository, ModelMapper modelMapper) {
        this.repository = repository;
        this.modelMapper = modelMapper;
    }

    public List<GetCollaboratorResponseDTO> getAll(){
        return repository.findAll() .stream()
                .map(dataModel -> {
                    GetCollaboratorResponseDTO dto = modelMapper.map(dataModel, GetCollaboratorResponseDTO.class);
                    modelMapper.map(dataModel.getPersonPII(), dto);
                    return dto;
                })
                .toList();
    }
}
