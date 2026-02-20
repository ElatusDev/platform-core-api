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
import com.akademiaplus.exception.CollaboratorDeletionNotAllowedException;
import com.akademiaplus.exception.CollaboratorNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeleteCollaboratorUseCase {
    private final CollaboratorRepository repository;
    private final TenantContextHolder tenantContextHolder;

    public DeleteCollaboratorUseCase(CollaboratorRepository repository,
                                      TenantContextHolder tenantContextHolder) {
        this.repository = repository;
        this.tenantContextHolder = tenantContextHolder;
    }

    public void delete(Long collaboratorId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException("Tenant context is required"));
        Optional<CollaboratorDataModel> found = repository.findById(
                new CollaboratorDataModel.CollaboratorCompositeId(tenantId, collaboratorId));
        if(found.isPresent()) {
            try {
                repository.delete(found.get());
            } catch (DataIntegrityViolationException ex) {
                throw new CollaboratorDeletionNotAllowedException(ex);
            }
        } else {
            throw new CollaboratorNotFoundException(String.valueOf(collaboratorId));
        }
    }
}
