/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.interfaceadapters;

import com.akademiaplus.collaborator.usecases.CollaboratorCreationUseCase;
import com.akademiaplus.collaborator.usecases.DeleteCollaboratorUseCase;
import com.akademiaplus.collaborator.usecases.GetAllCollaboratorsUseCase;
import com.akademiaplus.collaborator.usecases.GetCollaboratorByIdUseCase;

import openapi.akademiaplus.domain.user.management.api.CollaboratorsApi;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.GetCollaboratorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/user-management")
public class CollaboratorController implements CollaboratorsApi {

    private final GetAllCollaboratorsUseCase getAllCollaboratorsUseCase;
    private final CollaboratorCreationUseCase collaboratorCreationUseCase;
    private final GetCollaboratorByIdUseCase getCollaboratorByIdUseCase;
    private final DeleteCollaboratorUseCase deleteCollaboratorUseCase;

    public CollaboratorController(GetAllCollaboratorsUseCase getAllCollaboratorsUseCase,
                                  CollaboratorCreationUseCase collaboratorCreationUseCase,
                                  GetCollaboratorByIdUseCase getCollaboratorByIdUseCase,
                                  DeleteCollaboratorUseCase deleteCollaboratorUseCase) {
        this.getAllCollaboratorsUseCase = getAllCollaboratorsUseCase;
        this.collaboratorCreationUseCase = collaboratorCreationUseCase;
        this.getCollaboratorByIdUseCase = getCollaboratorByIdUseCase;
        this.deleteCollaboratorUseCase = deleteCollaboratorUseCase;
    }

    @Override
    public ResponseEntity<CollaboratorCreationResponseDTO> createCollaborator(
            CollaboratorCreationRequestDTO collaboratorCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(collaboratorCreationUseCase.create(collaboratorCreationRequestDTO));
    }

    @Override
    public ResponseEntity<Void> deleteCollaborator(Integer collaboratorId) {
        deleteCollaboratorUseCase.delete(collaboratorId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<GetCollaboratorResponseDTO> getCollaborator(Integer collaboratorId) {
        return ResponseEntity.ok(getCollaboratorByIdUseCase.get(collaboratorId));
    }

}
