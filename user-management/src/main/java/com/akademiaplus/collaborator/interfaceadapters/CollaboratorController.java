/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.interfaceadapters;

import com.akademiaplus.collaborator.usecases.CollaboratorCreationUseCase;
import com.akademiaplus.collaborator.usecases.CollaboratorUpdateUseCase;
import com.akademiaplus.collaborator.usecases.DeleteCollaboratorUseCase;
import com.akademiaplus.collaborator.usecases.GetAllCollaboratorsUseCase;
import com.akademiaplus.collaborator.usecases.GetCollaboratorByIdUseCase;
import openapi.akademiaplus.domain.user.management.api.CollaboratorsApi;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorUpdateRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorUpdateResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.GetAllCollaborators200ResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.GetCollaboratorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/user-management")
public class CollaboratorController implements CollaboratorsApi {

    private final GetAllCollaboratorsUseCase getAllCollaboratorsUseCase;
    private final CollaboratorCreationUseCase collaboratorCreationUseCase;
    private final CollaboratorUpdateUseCase collaboratorUpdateUseCase;
    private final GetCollaboratorByIdUseCase getCollaboratorByIdUseCase;
    private final DeleteCollaboratorUseCase deleteCollaboratorUseCase;

    public CollaboratorController(GetAllCollaboratorsUseCase getAllCollaboratorsUseCase,
                                  CollaboratorCreationUseCase collaboratorCreationUseCase,
                                  CollaboratorUpdateUseCase collaboratorUpdateUseCase,
                                  GetCollaboratorByIdUseCase getCollaboratorByIdUseCase,
                                  DeleteCollaboratorUseCase deleteCollaboratorUseCase) {
        this.getAllCollaboratorsUseCase = getAllCollaboratorsUseCase;
        this.collaboratorCreationUseCase = collaboratorCreationUseCase;
        this.collaboratorUpdateUseCase = collaboratorUpdateUseCase;
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
    public ResponseEntity<CollaboratorUpdateResponseDTO> updateCollaborator(
            Long collaboratorId, CollaboratorUpdateRequestDTO collaboratorUpdateRequestDTO) {
        return ResponseEntity.ok(collaboratorUpdateUseCase.update(collaboratorId, collaboratorUpdateRequestDTO));
    }

    @Override
    public ResponseEntity<Void> deleteCollaborator(Long collaboratorId) {
        deleteCollaboratorUseCase.delete(collaboratorId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<GetCollaboratorResponseDTO> getCollaborator(Long collaboratorId) {
        return ResponseEntity.ok(getCollaboratorByIdUseCase.get(collaboratorId));
    }

    @Override
    public ResponseEntity<GetAllCollaborators200ResponseDTO> getAllCollaborators(
            Integer page, Integer size, String skills,
            LocalDate entryDateFrom, LocalDate entryDateTo) {
        List<GetCollaboratorResponseDTO> collaborators = getAllCollaboratorsUseCase.getAll();
        GetAllCollaborators200ResponseDTO response = new GetAllCollaborators200ResponseDTO();
        response.setData(collaborators);
        return ResponseEntity.ok(response);
    }

}
